package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "earnings")
@Getter
@Setter
public class Earning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "earning_id")
    private Long earningId;

    @OneToOne(optional = false)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", referencedColumnName = "guide_id", nullable = false)
    private Guide guide;

    @Column(name = "admin_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal adminShare;

    @Column(name = "guide_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal guideShare;
}
