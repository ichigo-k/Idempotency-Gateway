package com.finsafe.idempotency_gateway.repositories;

import com.finsafe.idempotency_gateway.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction,UUID> {
}
