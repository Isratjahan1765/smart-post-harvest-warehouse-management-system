package com.sphwms.shared.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IdSequenceService {

    private final IdSequenceRepository sequenceRepository;

    @Transactional
    public String nextProductCode() {
        Long val = getNextAndIncrement("PRODUCT_CODE", 1L);
        return String.format("P%03d", val);
    }

    @Transactional
    public String nextBatchNo(LocalDate date) {
        Long val = getNextAndIncrement("BATCH_NO", 101L);
        int year = (date != null ? date.getYear() : LocalDate.now().getYear());
        return String.format("B-%d-%d", year, val);
    }

    @Transactional
    public String nextSaleNo() {
        Long val = getNextAndIncrement("SALE_NO", 1L);
        return String.format("S-%04d", val);
    }

    @Transactional
    public String nextDonationNo() {
        Long val = getNextAndIncrement("DONATION_NO", 1L);
        return String.format("D-%04d", val);
    }

    @Transactional
    public String nextTripNo() {
        Long val = getNextAndIncrement("TRIP_NO", 1L);
        return String.format("T-%03d", val);
    }

    @Transactional
    public WarehouseCodeResult nextWarehouseCode() {
        Long val = getNextAndIncrement("WAREHOUSE_CODE", 1L);
        String code = String.format("WH-%02d", val);
        return new WarehouseCodeResult(val.intValue(), code);
    }

    @Transactional(readOnly = true)
    public String peekWarehouseCode() {
        Long val = sequenceRepository.findBySeqName("WAREHOUSE_CODE")
                .map(IdSequence::getNextVal)
                .orElse(1L);
        return String.format("WH-%02d", val);
    }

    @Transactional(readOnly = true)
    public String peekBatchNo(LocalDate date) {
        Long val = sequenceRepository.findBySeqName("BATCH_NO")
                .map(IdSequence::getNextVal)
                .orElse(101L);
        int year = (date != null ? date.getYear() : LocalDate.now().getYear());
        return String.format("B-%d-%d", year, val);
    }

    @Transactional(readOnly = true)
    public String peekSaleNo() {
        Long val = sequenceRepository.findBySeqName("SALE_NO")
                .map(IdSequence::getNextVal)
                .orElse(1L);
        return String.format("S-%04d", val);
    }

    private Long getNextAndIncrement(String seqName, Long defaultStart) {
        IdSequence seq = sequenceRepository.findBySeqNameWithLock(seqName)
                .orElseGet(() -> new IdSequence(seqName, defaultStart));
        Long current = seq.getNextVal();
        seq.setNextVal(current + 1);
        sequenceRepository.save(seq);
        return current;
    }

    public record WarehouseCodeResult(int codeNumber, String code) {}
}
