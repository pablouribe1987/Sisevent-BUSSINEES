package pe.gob.mef.sisevent.bs.mem;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.springframework.stereotype.Service;

/**
 * This memory warning system will call the listener when we
 * exceed the percentage of available memory specified.  There
 * should only be one instance of this object created, since the
 * usage threshold can only be set to one number.
 */
@Service
public class MemoryWarningSystem {
	
  private final Collection<Listener> listeners = new ArrayList<Listener>();

  public MemoryWarningSystem() {
    MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
    NotificationEmitter emitter = (NotificationEmitter) mbean;
    emitter.addNotificationListener(new NotificationListener() {
      public void handleNotification(Notification n, Object hb) {
        if (n.getType().equals(
            MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
          long maxMemory = tenuredGenPool.getUsage().getMax();
          long usedMemory = tenuredGenPool.getUsage().getUsed();
          for (Listener listener : listeners) {
            listener.memoryUsageLow(usedMemory, maxMemory);
          }
        }
      }
    }, null, null);
  }
  
  @PostConstruct
  public void inicialize(){
	  Listener l = new MemoryWarningListenerImpl();
	  addListener(l);
	  setPercentageUsageThreshold(0.7D);
	  System.out.println("Configurci�n completa");
  }

  public boolean addListener(Listener listener) {
    return listeners.add(listener);
  }

  public boolean removeListener(Listener listener) {
    return listeners.remove(listener);
  }

  private static final MemoryPoolMXBean tenuredGenPool =
      findTenuredGenPool();

  public static void setPercentageUsageThreshold(double percentage) {
    if (percentage <= 0.0 || percentage > 1.0) {
      throw new IllegalArgumentException("Percentage not in range");
    }
    long maxMemory = tenuredGenPool.getUsage().getMax();
    long warningThreshold = (long) (maxMemory * percentage);
    tenuredGenPool.setUsageThreshold(warningThreshold);
  }

  /**
   * Tenured Space Pool can be determined by it being of type
   * HEAP and by it being possible to set the usage threshold.
   */
  private static MemoryPoolMXBean findTenuredGenPool() {
    for (MemoryPoolMXBean pool :
        ManagementFactory.getMemoryPoolMXBeans()) {
      // I don't know whether this approach is better, or whether
      // we should rather check for the pool name "Tenured Gen"?
      if (pool.getType() == MemoryType.HEAP &&
          pool.isUsageThresholdSupported()) {
        return pool;
      }
    }
    throw new AssertionError("Could not find tenured space");
  }
}