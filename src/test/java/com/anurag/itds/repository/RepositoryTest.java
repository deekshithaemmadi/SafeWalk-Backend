package com.anurag.itds.repository;

import com.anurag.itds.entity.CommunityReport;
import com.anurag.itds.entity.EmergencyContact;
import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.entity.ReportStatus;
import com.anurag.itds.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JPA slice tests on the isolated H2 database (schema generated from the entities). */
@DataJpaTest
class RepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private EmergencyContactRepository contactRepository;
    @Autowired private CommunityReportRepository reportRepository;
    @Autowired private HelpLocationRepository helpLocationRepository;

    private User user(String email, String phone) {
        return userRepository.saveAndFlush(new User("Repo User", email, phone, "hash"));
    }

    @Test
    void userLookupsAreCaseInsensitiveAndUniqueConstraintsHold() {
        user("repo-a@test.local", "9700000001");
        assertThat(userRepository.findByEmailIgnoreCase("REPO-A@test.local")).isPresent();
        assertThat(userRepository.existsByPhone("9700000001")).isTrue();
        assertThatThrownBy(() -> user("repo-a@test.local", "9700000002"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void contactsAreScopedToTheirOwner() {
        User a = user("repo-b@test.local", "9700000011");
        User b = user("repo-c@test.local", "9700000012");
        EmergencyContact contact = contactRepository.save(new EmergencyContact(a, "Mom", "9700000013", "Mother"));
        assertThat(contactRepository.countByUserId(a.getId())).isEqualTo(1);
        assertThat(contactRepository.findByIdAndUserId(contact.getId(), b.getId())).isEmpty();
        assertThat(contactRepository.existsByUserIdAndPhone(a.getId(), "9700000013")).isTrue();
        assertThat(contactRepository.existsByUserIdAndPhone(b.getId(), "9700000013")).isFalse();
    }

    @Test
    void permittedReportsHonourVisibilityAndStatusFilter() {
        User owner = user("repo-d@test.local", "9700000021");
        User other = user("repo-e@test.local", "9700000022");
        CommunityReport pending = reportRepository.save(new CommunityReport(owner, "Poor lighting", "Street light broken near bus stop"));
        CommunityReport verified = reportRepository.save(new CommunityReport(owner, "Harassment", "Reported catcalling near the market"));
        verified.setStatus(ReportStatus.VERIFIED);
        reportRepository.saveAndFlush(verified);

        var forOther = reportRepository.findPermitted(other.getId(), ReportStatus.VERIFIED, null, null, PageRequest.of(0, 10));
        assertThat(forOther.getContent()).extracting(CommunityReport::getId).containsExactly(verified.getId());

        var ownerPending = reportRepository.findPermitted(owner.getId(), ReportStatus.VERIFIED, ReportStatus.PENDING, null, PageRequest.of(0, 10));
        assertThat(ownerPending.getContent()).extracting(CommunityReport::getId).containsExactly(pending.getId());
    }

    @Test
    void helpLocationSourceKeyDeduplicates() {
        helpLocationRepository.saveAndFlush(new HelpLocation(HelpLocationType.POLICE, "PS One", "key-1"));
        assertThat(helpLocationRepository.findBySourceKey("key-1")).isPresent();
        assertThatThrownBy(() -> helpLocationRepository.saveAndFlush(new HelpLocation(HelpLocationType.POLICE, "PS One dup", "key-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
