package com.jwellkeeper.billing.repository;

import com.jwellkeeper.billing.model.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, UUID> {
}
