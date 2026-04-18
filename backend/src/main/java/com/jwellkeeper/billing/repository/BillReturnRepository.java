package com.jwellkeeper.billing.repository;

import com.jwellkeeper.billing.model.BillReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillReturnRepository extends JpaRepository<BillReturn, UUID> {
}
