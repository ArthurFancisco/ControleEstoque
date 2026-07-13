package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.Subscription;
import br.com.flowstock.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    long countByPaymentStatus(PaymentStatus paymentStatus);
}
