package com.sphwms.alert.internal.repository;

import com.sphwms.alert.internal.entity.AlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertSettingRepository extends JpaRepository<AlertSetting, Integer> {
}
