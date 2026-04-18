package com.jwellkeeper.billing.repository;

import com.jwellkeeper.billing.model.BillExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillExchangeRepository extends JpaRepository<BillExchange, UUID> {
}
