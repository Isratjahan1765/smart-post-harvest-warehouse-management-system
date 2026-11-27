package com.sphwms.driver.internal.repository;

import com.sphwms.driver.internal.entity.Driver;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findAllByOrderByDriverIdAsc();

    Optional<Driver> findByLicenseNo(String licenseNo);

    Optional<Driver> findByPhone(String phone);

    @Query("SELECT d FROM Driver d WHERE d.licenseNo = :licenseNo AND d.driverId <> :id")
    Optional<Driver> findByLicenseNoAndIdNot(@Param("licenseNo") String licenseNo, @Param("id") Long id);

    @Query("SELECT d FROM Driver d WHERE d.phone = :phone AND d.driverId <> :id")
    Optional<Driver> findByPhoneAndIdNot(@Param("phone") String phone, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d WHERE d.driverId = :id")
    Optional<Driver> findByIdWithLock(@Param("id") Long id);

    List<Driver> findByActiveTrueOrderByDriverIdAsc();
}
