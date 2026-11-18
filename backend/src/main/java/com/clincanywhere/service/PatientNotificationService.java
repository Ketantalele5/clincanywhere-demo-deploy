package com.clincanywhere.service;

import com.clincanywhere.entity.PatientNotificationItem;
import com.clincanywhere.repository.PatientNotificationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service public class PatientNotificationService {
 private final PatientNotificationRepository repo;private final List<PatientNotificationProvider> providers;
 public PatientNotificationService(PatientNotificationRepository repo,ObjectProvider<PatientNotificationProvider> providers){this.repo=repo;this.providers=providers.orderedStream().toList();}
 @Transactional public PatientNotificationItem enqueue(String clinic,String actor,String destination,String channel,String message){var item=repo.save(new PatientNotificationItem(clinic,actor,channel,destination,message));try{var provider=providers.stream().filter(p->p.supports(channel)).findFirst().orElseThrow(()->new IllegalStateException("No configured "+channel+" provider"));item.delivered(provider.send(channel,destination,message));}catch(RuntimeException failure){item.failed(failure.getClass().getSimpleName());}return item;}
 @Transactional(readOnly=true) public List<Map<String,Object>> list(String clinic){return repo.findTop100ByClinicIdOrderByIdDesc(clinic).stream().map(item->Map.<String,Object>of("id",item.id(),"channel",item.channel(),"status",item.status(),"createdAt",item.getCreatedAt())).toList();}
}
