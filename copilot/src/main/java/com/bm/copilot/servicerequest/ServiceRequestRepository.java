package com.bm.copilot.servicerequest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for service requests. Spring generates the implementation at startup. */
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<ServiceRequest> findByStatusOrderBySubmittedAtAsc(RequestStatus status);
}
