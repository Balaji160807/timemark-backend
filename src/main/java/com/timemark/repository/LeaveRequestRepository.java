package com.timemark.repository;

import com.timemark.entity.Employee;
import com.timemark.entity.LeaveRequest;
import com.timemark.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeOrderByAppliedOnDesc(Employee employee);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByEmployeeAndStatus(Employee employee, LeaveStatus status);
}
