package hr.algebra.parkalot.services;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.dtos.ViolationUpdateForm;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.EmissionClass;
import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.repositories.ParkingCheckLogRepo;
import hr.algebra.parkalot.repositories.PermitArchiveRepo;
import hr.algebra.parkalot.repositories.PermitRepo;
import hr.algebra.parkalot.schedulers.CleanerUpperJob;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.db.ViolationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Service-level integration tests: permit expiry, scheduled cleanup and the remaining service helpers. */
class ParkingAndCleanupTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private ParkingService parkingService;
    @Autowired private CleanerUpperJob cleanerUpperJob;
    @Autowired private AreaService areaService;
    @Autowired private ViolationService violationService;
    @Autowired private PermitRepo permitRepo;
    @Autowired private PermitArchiveRepo archiveRepo;
    @Autowired private ParkingCheckLogRepo logRepo;

    private User user;
    private User officer;
    private Vehicle vehicle;
    private Area area;

    @BeforeEach
    void setUp() {
        user = data.user("user@test.com", Role.USER);
        officer = data.user("officer@test.com", Role.OFFICER);
        vehicle = data.vehicle("ZG1111AA", EmissionClass.EURO_6, user);
        area = data.area("Centar", 15, 45, 1);
    }

    @Test
    void checkParkingStatus_returnsActivePermit_andCullsExpiredOnes() {
        Permit expired = data.permit(vehicle, area, user, LocalDate.now().minusDays(10), PermitDuration.DAY, true);
        Permit active = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.WEEK, true);
        data.flushAndClear();
        Area reloadedArea = areaService.findById(area.getId()).orElseThrow();

        assertThat(parkingService.checkParkingStatusIsValid("ZG1111AA", reloadedArea))
                .map(Permit::getId).contains(active.getId());
        data.flushAndClear();

        assertThat(permitRepo.existsById(expired.getId())).isFalse();
        assertThat(archiveRepo.findPermitArchiveByLicencePlate("ZG1111AA")).hasSize(1);
    }

    @Test
    void checkParkingStatus_unpaidOrFuturePermit_isNotValid() {
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, false);
        data.permit(vehicle, area, user, LocalDate.now().plusDays(5), PermitDuration.DAY, true);
        data.flushAndClear();
        Area reloadedArea = areaService.findById(area.getId()).orElseThrow();

        assertThat(parkingService.checkParkingStatusIsValid("ZG1111AA", reloadedArea)).isEmpty();
    }

    @Test
    void cullAllPermits_archivesEveryExpiredPermit() {
        data.permit(vehicle, area, user, LocalDate.now().minusDays(10), PermitDuration.DAY, true);
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.WEEK, true);
        data.flushAndClear();

        parkingService.cullAllPermits();
        data.flushAndClear();

        assertThat(permitRepo.findAll()).hasSize(1);
        assertThat(archiveRepo.count()).isEqualTo(1);
    }

    @Test
    void generalCleanup_archivesExpiredPermits() {
        data.permit(vehicle, area, user, LocalDate.now().minusDays(10), PermitDuration.DAY, true);
        data.flushAndClear();

        cleanerUpperJob.generalCleanup();
        data.flushAndClear();

        assertThat(permitRepo.count()).isZero();
        assertThat(archiveRepo.count()).isEqualTo(1);
    }

    @Test
    void logCleanup_keepsLogsWithoutViolationEvenWhenOld() {
        ParkingCheckLog recent = data.parkingLog(officer, "ZG1111AA", area, LocalDateTime.now());
        ParkingCheckLog oldWithoutViolation = data.parkingLog(officer, "ZG1111AA", area, LocalDateTime.now().minusMonths(3));
        data.flushAndClear();

        cleanerUpperJob.generalCleanup();
        data.flushAndClear();

        assertThat(logRepo.existsById(recent.getId())).isTrue();
        assertThat(logRepo.existsById(oldWithoutViolation.getId())).isTrue();
    }

    @Test
    void clearUnpaidPermits_deletesOnlyStaleUnpaid() {
        Permit stale = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, false);
        stale.setCreatedAt(LocalDateTime.now().minusHours(2));
        permitRepo.saveAndFlush(stale);
        Permit fresh = data.permit(vehicle, area, user, LocalDate.now().plusDays(10), PermitDuration.DAY, false);
        Permit paid = data.permit(vehicle, area, user, LocalDate.now().plusDays(20), PermitDuration.DAY, true);
        paid.setCreatedAt(LocalDateTime.now().minusHours(2));
        permitRepo.saveAndFlush(paid);
        data.flushAndClear();

        cleanerUpperJob.clearUnpaidPermits();
        data.flushAndClear();

        assertThat(permitRepo.existsById(stale.getId())).isFalse();
        assertThat(permitRepo.existsById(fresh.getId())).isTrue();
        assertThat(permitRepo.existsById(paid.getId())).isTrue();
    }

    @Test
    void calculateAreaPermitPrice_usesZonePriceTimesDurationMultiplier() {
        assertThat(areaService.calculateAreaPermitPrice(area.getId(), PermitDuration.WEEK)).contains(15.0 * 6);
        assertThat(areaService.calculateAreaPermitPrice(999999L, PermitDuration.DAY)).isEmpty();
    }

    @Test
    void updateViolation_unknownId_throwsEntityNotFound() {
        ViolationUpdateForm form = new ViolationUpdateForm(999999L, null, "m", null, null, null, null, null, true);

        assertThatThrownBy(() -> violationService.updateViolation(form)).isInstanceOf(EntityNotFoundException.class);
    }
}
