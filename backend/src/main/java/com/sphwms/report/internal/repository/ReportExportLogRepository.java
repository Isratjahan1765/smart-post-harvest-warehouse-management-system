package com.sphwms.report.internal.repository;

import com.sphwms.report.internal.entity.ReportExportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportExportLogRepository extends JpaRepository<ReportExportLog, Long> {
}
