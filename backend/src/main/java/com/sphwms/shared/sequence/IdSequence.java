package com.sphwms.shared.sequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "id_sequence")
@Getter
@Setter
@NoArgsConstructor
public class IdSequence {

    @Id
    @Column(name = "seq_name", length = 30)
    private String seqName;

    @Column(name = "next_val", nullable = false)
    private Long nextVal;

    public IdSequence(String seqName, Long nextVal) {
        this.seqName = seqName;
        this.nextVal = nextVal;
    }
}
