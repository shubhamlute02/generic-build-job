//package com.accrevent.radius.scheduling;
//
//import com.accrevent.radius.model.Task;
//import com.accrevent.radius.model.UserRegion;
//import com.accrevent.radius.service.TaskService;
//import com.accrevent.radius.service.UserRegionService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class TaskMoveScheduler {
//
//    private final UserRegionService userRegionService;
//    private final TaskService taskService;
//    Logger logger = LoggerFactory.getLogger(TaskMoveScheduler.class);
//    public TaskMoveScheduler(UserRegionService userRegionService, TaskService taskService) {
//        this.userRegionService = userRegionService;
//        this.taskService = taskService;
//    }
//
//    @Scheduled(cron = "0 30 14 * * ?") //for IST (server is in utc pacific time)
//    public void ISTTaskMove()
//    {
//        logger.info("ISTTaskMove service started");
//        try {
//            List<String> userId = userRegionService.getUserListByRegion("IST")
//                    .stream()
//                    .map(UserRegion::getUserId)
//                    .collect(Collectors.toList());
//            logger.info("ISTTaskMove service fount users list size = " + userId.size());
//            if (!userId.isEmpty()) {
//                List<String> statusList = List.of("Not started", "In work");
//                List<Long> taskIds = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate
//                                (userId, statusList, System.currentTimeMillis())
//                        .stream().map(Task::getTaskId).collect(Collectors.toList());
//
//                logger.info("ISTTaskMove service fount taskIds list size = " + taskIds.size());
//                if (!taskIds.isEmpty()) {
//                    ZonedDateTime newDate6Am = ZonedDateTime.
//                            now(ZoneId.of("Asia/Kolkata")).withHour(6).withMinute(0).withSecond(0).withNano(0);
//                    String status = taskService.updateMultipleTasksDueDates(taskIds, newDate6Am.withZoneSameInstant(ZoneId.of("UTC")));
//                    logger.info("ISTTaskMove service final output = " + status);
//                }
//            }
//        }catch(Exception e){
//            logger.error("ISTTaskMove service error ="+e.getMessage());
//            e.printStackTrace();
//        }
//        logger.info("ISTTaskMove service finished");
//    }
//
//    @Scheduled(cron = "0 0 20 * * ?") //for GMT (server is in utc pacific time)
//    public void GMTTaskMove()
//    {
//        logger.info("GMTTaskMove service started");
//        try {
//            List<String> userId = userRegionService.getUserListByRegion("GMT")
//                    .stream()
//                    .map(UserRegion::getUserId)
//                    .collect(Collectors.toList());
//            logger.info("GMTTaskMove service fount users list size = " + userId.size());
//            if (!userId.isEmpty()) {
//                List<String> statusList = List.of("Not started", "In work");
//                List<Long> taskIds = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate
//                                (userId, statusList, ZonedDateTime.now(ZoneId.of("UTC")))
//                        .stream().map(Task::getTaskId).collect(Collectors.toList());
//                logger.info("GMTTaskMove service fount taskIds list size = " + taskIds.size());
//                if (!taskIds.isEmpty()) {
//                    ZonedDateTime newDate6Am = ZonedDateTime.
//                            now(ZoneId.of("UTC")).withHour(6).withMinute(0).withSecond(0).withNano(0);
//                    String status = taskService.updateMultipleTasksDueDates(taskIds, newDate6Am);
//                    logger.info("GMTTaskMove service final output = " + status);
//                }
//            }
//        }catch(Exception e){
//            logger.error(" GMTTaskMove service error ="+e.getMessage());
//            e.printStackTrace();
//        }
//        logger.info("GMTTaskMove service finished");
//    }
//
//    @Scheduled(cron = "0 0 0 * * ?") //for EST (server is in utc pacific time)
//    public void ESTTaskMove()
//    {
//        logger.info("ESTTaskMove service started");
//        try {
//            List<String> userId = userRegionService.getUserListByRegion("EST")
//                    .stream()
//                    .map(UserRegion::getUserId)
//                    .collect(Collectors.toList());
//            logger.info("ESTTaskMove service fount users list size = " + userId.size());
//            if (!userId.isEmpty()) {
//                List<String> statusList = List.of("Not started", "In work");
//                List<Long> taskIds = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate
//                                (userId, statusList, ZonedDateTime.now(ZoneId.of("UTC")))
//                        .stream().map(Task::getTaskId).collect(Collectors.toList());
//                logger.info("ESTTaskMove service fount taskIds list size = " + taskIds.size());
//                if (!taskIds.isEmpty()) {
//                    ZonedDateTime newDate6Am = ZonedDateTime.
//                            now(ZoneId.of("America/New_York")).withHour(6).withMinute(0).withSecond(0).withNano(0);
//                    String status = taskService.updateMultipleTasksDueDates(taskIds, newDate6Am.withZoneSameInstant(ZoneId.of("UTC")));
//                    logger.info("ESTTaskMove service final output = " + status);
//                }
//            }
//        }catch(Exception e){
//            logger.error("ESTTaskMove service error ="+e.getMessage());
//            e.printStackTrace();
//        }
//        logger.info("ESTTaskMove service finished");
//    }
//}
////
////package com.accrevent.radius.scheduling;
////
////import com.accrevent.radius.model.Task;
////import com.accrevent.radius.model.UserRegion;
////import com.accrevent.radius.service.TaskService;
////import com.accrevent.radius.service.UserRegionService;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.scheduling.annotation.Scheduled;
////import org.springframework.stereotype.Component;
////
////import java.time.ZoneId;
////import java.time.ZonedDateTime;
////import java.util.List;
////import java.util.stream.Collectors;
////
////@Component
////public class TaskMoveScheduler {
////
////    private final UserRegionService userRegionService;
////    private final TaskService taskService;
////    Logger logger = LoggerFactory.getLogger(TaskMoveScheduler.class);
////
////    public TaskMoveScheduler(UserRegionService userRegionService, TaskService taskService) {
////        this.userRegionService = userRegionService;
////        this.taskService = taskService;
////    }
////
////    @Scheduled(cron = "0 30 14 * * ?") // for IST
////    public void ISTTaskMove() {
////        logger.info("ISTTaskMove service started");
////        try {
////            List<String> userId = userRegionService.getUserListByRegion("IST")
////                    .stream()
////                    .map(UserRegion::getUserId)
////                    .collect(Collectors.toList());
////
////            logger.info("ISTTaskMove found users: " + userId.size());
////            if (!userId.isEmpty()) {
////                List<String> statusList = List.of("Not started", "In work");
////                Long currentTimeMillis = System.currentTimeMillis();
////
////                List<Long> taskIds = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate(userId, statusList, currentTimeMillis)
////                        .stream().map(Task::getTaskId).collect(Collectors.toList());
////
////                logger.info("ISTTaskMove found taskIds: " + taskIds.size());
////                if (!taskIds.isEmpty()) {
////                    Long newDate6AmMillis = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
////                            .withHour(6).withMinute(0).withSecond(0).withNano(0)
////                            .toInstant().toEpochMilli();
////
////                    String status = taskService.updateMultipleTasksDueDates(taskIds, newDate6AmMillis);
////                    logger.info("ISTTaskMove final output: " + status);
////                }
////            }
////        } catch (Exception e) {
////            logger.error("ISTTaskMove error: " + e.getMessage(), e);
////        }
////        logger.info("ISTTaskMove service finished");
////    }
////
////    @Scheduled(cron = "0 0 20 * * ?") // for GMT
////    public void GMTTaskMove() {
////        logger.info("GMTTaskMove service started");
////        try {
////            List<String> userId = userRegionService.getUserListByRegion("GMT")
////                    .stream()
////                    .map(UserRegion::getUserId)
////                    .collect(Collectors.toList());
////
////            logger.info("GMTTaskMove found users: " + userId.size());
////            if (!userId.isEmpty()) {
////                List<String> statusList = List.of("Not started", "In work");
////                Long currentTimeMillis = System.currentTimeMillis();
////
////                List<Long> taskIds = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate(userId, statusList, currentTimeMillis)
////                        .stream().map(Task::getTaskId).collect(Collectors.toList());
////
////                logger.info("GMTTaskMove found taskIds: " + taskIds.size());
////                if (!taskIds.isEmpty()) {
////                    Long newDate6AmMillis = ZonedDateTime.now(ZoneId.of("UTC"))
////                            .withHour(6).withMinute(0).withSecond(0).withNano(0)
////                            .toInstant().toEpochMilli();
////
////                    String status = taskService.updateMultipleTasksDueDates(taskIds, newDate6AmMillis);
////                    logger.info("GMTTaskMove final output: " + status);
////                }
////            }
////        } catch (Exception e) {
////            logger.error("GMTTaskMove error: " + e.getMessage(), e);
////        }
////        logger.info("GMTTaskMove service finished");
////    }
////
////    @Scheduled(cron = "0 0 0 * * ?") // for EST
////    public void ESTTaskMove() {
////        logger.info("ESTTaskMove service started");
////        try {
////            List<String> userId = userRegionService.getUserListByRegion("EST")
////                    .stream()
////                    .map(UserRegion::getUserId)
////                    .collect(Collectors.toList());
////
////            logger.info("ESTTaskMove found users: " + userId.size());
////            if (!userId.isEmpty()) {
////                List<String> statusList = List.of("Not started", "In work");
////                Long currentTimeMillis = System.currentTimeMillis();
////
////                List<Long> taskIds = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate(userId, statusList, currentTimeMillis)
////                        .stream().map(Task::getTaskId).collect(Collectors.toList());
////
////                logger.info("ESTTaskMove found taskIds: " + taskIds.size());
////                if (!taskIds.isEmpty()) {
////                    Long newDate6AmMillis = ZonedDateTime.now(ZoneId.of("America/New_York"))
////                            .withHour(6).withMinute(0).withSecond(0).withNano(0)
////                            .toInstant().toEpochMilli();
////
////                    String status = taskService.updateMultipleTasksDueDates(taskIds, newDate6AmMillis);
////                    logger.info("ESTTaskMove final output: " + status);
////                }
////            }
////        } catch (Exception e) {
////            logger.error("ESTTaskMove error: " + e.getMessage(), e);
////        }
////        logger.info("ESTTaskMove service finished");
////    }
////}
////
//
//package com.accrevent.radius.scheduling;
//
//import com.accrevent.radius.model.Task;
//import com.accrevent.radius.model.UserRegion;
//import com.accrevent.radius.service.TaskService;
//import com.accrevent.radius.service.UserRegionService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//@Component
//public class TaskMoveScheduler {
//
//    private final UserRegionService userRegionService;
//    private final TaskService taskService;
//    Logger logger = LoggerFactory.getLogger(TaskMoveScheduler.class);
//
//    public TaskMoveScheduler(UserRegionService userRegionService, TaskService taskService) {
//        this.userRegionService = userRegionService;
//        this.taskService = taskService;
//    }
//
//    @Scheduled(cron = "0 30 02 * * ?", zone = "Asia/Kolkata")
//    public void ISTTaskMove() {
//        ZonedDateTime schedulerStartTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//        logger.info("ISTTaskMove: Scheduler triggered at {}", schedulerStartTime);
//
//        try {
//            List<UserRegion> userRegions = userRegionService.getUserListByRegion("IST");
//            List<String> userIds = userRegions.stream().map(UserRegion::getUserId).collect(Collectors.toList());
//
//            logger.debug("User found in IST = {}", userIds.size());
//            userRegions.forEach(user -> logger.debug("Next user in scheduler - {}", user.getUserId()));
//
//            if (userIds.isEmpty()) {
//                logger.warn("ISTTaskMove: No users found in IST region, skipping task movement.");
//                return;
//            }
//
//            List<String> statusList = List.of("Not started", "In work");
//
//            long now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
//
//            List<Task> tasks = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate(userIds, statusList, now);
//            logger.debug("task found for 'In work' and 'Not started' status = {}", tasks.size());
//
//            //  Log each task's id, name, and due date
//            for (Task task : tasks) {
//                LocalDateTime dueDateTime = Instant.ofEpochMilli(task.getDueDate())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime();
//
//                logger.debug("task found for 'In work' and 'Not started' status with INFO: ID={}, Name='{}', DueDate={} ",
//                        task.getTaskId(),
//                        task.getTaskName(),
//                        dueDateTime,
//                        task.getDueDate());
//            }
//
//            List<Task> overdueTasks = tasks.stream()
//                    .filter(task -> task.getDueDate() <= now)
//                    .collect(Collectors.toList());
//
//            logger.debug("Now = {}", Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
//
//            logger.debug("task found who passed the due date are = {}", overdueTasks.size());
//
//            for (Task task : overdueTasks) {
//                LocalDateTime dueDateTime = Instant.ofEpochMilli(task.getDueDate())
//                        .atZone(ZoneId.of("Asia/Kolkata"))
//                        .toLocalDateTime();
//
//                logger.debug("Task who passed due date are: ID={}, Name='{}', DueDate={}",
//                        task.getTaskId(),
//                        task.getTaskName(),
//                        dueDateTime,
//                        task.getDueDate());
//            }
//
//            if (overdueTasks.isEmpty()) {
//                logger.info("ISTTaskMove: No eligible overdue tasks to update, scheduler finished.");
//                return;
//            }
//            long newDueDateMillis = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
//                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
//                    .toInstant().toEpochMilli();
//            LocalDateTime newDueDateReadable = Instant.ofEpochMilli(newDueDateMillis)
//                    .atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
//
//            for (Task t : overdueTasks) {
//                LocalDateTime currentDueDateReadable = Instant.ofEpochMilli(t.getDueDate())
//                        .atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
//                logger.info("Moving taskId={}, taskName='{}', currentDueDate='{}', to newDueDate='{}'",
//                        t.getTaskId(), t.getTaskName(), currentDueDateReadable, newDueDateReadable);
//            }
//
//            List<Long> taskIds = overdueTasks.stream().map(Task::getTaskId).collect(Collectors.toList());
//            String updateStatus = taskService.updateMultipleTasksDueDates(taskIds, newDueDateMillis);
//
//            logger.info("Successfully passed {} task(s) for the next day at 06:00 IST", taskIds.size());
//        } catch (Exception e) {
//            logger.error("ISTTaskMove: An error occurred during task movement: {}", e.getMessage(), e);
//        }finally {
//            ZonedDateTime end = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//            logger.info("ISTTaskMove: Scheduler finished at {}", end);
//        }
//    }
//
//    @Scheduled(cron = "0 30 02 * * ?", zone = "GMT")
//    public void GMTTaskMove() {
//        ZonedDateTime schedulerStartTime = ZonedDateTime.now(ZoneId.of("GMT"));
//        logger.info("GMTTaskMove: Scheduler triggered at {}", schedulerStartTime);
//        try {
//            // ✅ Include all target regions
//            List<String> allowedRegions = List.of("IST", "GMT", "EST");
//
//            List<UserRegion> userRegions = new ArrayList<>();
//            for (String region : allowedRegions) {
//                List<UserRegion> regionUsers = userRegionService.getUserListByRegion(region);
//                logger.debug("Found {} users in region {}", regionUsers.size(), region);
//                userRegions.addAll(regionUsers);
//            }
//
//            List<String> userIds = userRegions.stream()
//                    .map(UserRegion::getUserId)
//                    .collect(Collectors.toList());
//
//
//            logger.debug("Total users across IST, GMT, EST = {}", userIds.size());
//            userIds.forEach(id -> logger.debug("Included userId = {}", id));
//
//            userRegions.forEach(user -> logger.debug("Next user in scheduler - {}", user.getUserId()));
//
//            if (userIds.isEmpty()) {
//                logger.warn("GMTTaskMove: No users found in GMT region, skipping task movement.");
//                return;
//            }
//
//            List<String> statusList = List.of("Not started", "In work");
//
//            //  Always get 'now' in GMT regardless of server timezone
//            long now = ZonedDateTime.now(ZoneId.of("GMT")).toInstant().toEpochMilli();
//
//            List<Task> tasks = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate(userIds, statusList, now);
//            logger.debug("task found for 'In work' and 'Not started' status = {}", tasks.size());
//
//            for (Task task : tasks) {
//                LocalDateTime dueDateTime = Instant.ofEpochMilli(task.getDueDate())
//                        .atZone(ZoneId.of("GMT")).toLocalDateTime();
//
//                logger.debug("task found for 'In work' and 'Not started' status with INFO: ID={}, Name='{}', DueDate={}",
//                        task.getTaskId(), task.getTaskName(), dueDateTime);
//            }
//
//            List<Task> overdueTasks = tasks.stream()
//                    .filter(task -> task.getDueDate() <= now)
//                    .collect(Collectors.toList());
//
//            // Log 'now' in human-readable GMT for validation
//            logger.debug("Now (GMT) = {}",
//                    Instant.ofEpochMilli(now).atZone(ZoneId.of("GMT")).toLocalDateTime());
//
//
//            logger.debug("task found who passed the due date are = {}", overdueTasks.size());
//
//            for (Task task : overdueTasks) {
//                LocalDateTime dueDateTime = Instant.ofEpochMilli(task.getDueDate())
//                        .atZone(ZoneId.of("GMT")).toLocalDateTime();
//
//                logger.debug("Task who passed due date are: ID={}, Name='{}', DueDate={}",
//                        task.getTaskId(), task.getTaskName(), dueDateTime);
//
//            }
//
//            if (overdueTasks.isEmpty()) {
//                logger.info("GMTTaskMove: No eligible overdue tasks to update, scheduler finished.");
//                return;
//            }
//
//            long newDueDateMillis = ZonedDateTime.now(ZoneId.of("GMT"))
//                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
//                    .toInstant().toEpochMilli();
//
//            LocalDateTime newDueDateReadable = Instant.ofEpochMilli(newDueDateMillis)
//                    .atZone(ZoneId.of("GMT")).toLocalDateTime();
//
//            for (Task t : overdueTasks) {
//                LocalDateTime currentDueDateReadable = Instant.ofEpochMilli(t.getDueDate())
//                        .atZone(ZoneId.of("GMT")).toLocalDateTime();
//                logger.info("Moving taskId={}, taskName='{}', currentDueDate='{}', to newDueDate='{}'",
//                        t.getTaskId(), t.getTaskName(), currentDueDateReadable, newDueDateReadable);
//            }
//
//            List<Long> taskIds = overdueTasks.stream().map(Task::getTaskId).collect(Collectors.toList());
//            String updateStatus = taskService.updateMultipleTasksDueDates(taskIds, newDueDateMillis);
//
//            logger.info("Successfully passed {} task(s) for the next day at 06:00 GMT", taskIds.size());
//        } catch (Exception e) {
//            logger.error("GMTTaskMove: An error occurred during task movement: {}", e.getMessage(), e);
//        } finally {
//            ZonedDateTime end = ZonedDateTime.now(ZoneId.of("GMT"));
//            logger.info("GMTTaskMove: Scheduler finished at {}", end);
//        }
//    }
//
//    @Scheduled(cron = "0 30 02 * * ?", zone = "America/New_York")
//    public void ESTTaskMove() {
//        ZonedDateTime schedulerStartTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
//        logger.info("ESTTaskMove: Scheduler triggered at {}", schedulerStartTime);
//        try {
//            List<UserRegion> userRegions = userRegionService.getUserListByRegion("EST");
//            List<String> userIds = userRegions.stream().map(UserRegion::getUserId).collect(Collectors.toList());
//
//            logger.debug("User found in EST = {}", userIds.size());
//            userRegions.forEach(user -> logger.debug("Next user in scheduler - {}", user.getUserId()));
//
//            if (userIds.isEmpty()) {
//                logger.warn("ESTTaskMove: No users found in EST region, skipping task movement.");
//                return;
//            }
//
//            List<String> statusList = List.of("Not started", "In work");
//
//
//            // Get 'now' in EST regardless of server timezone
//            long now = ZonedDateTime.now(ZoneId.of("America/New_York")).toInstant().toEpochMilli();
//
//
//            List<Task> tasks = taskService.findTasksByAssignToAndStatusAndBeforeGivenDate(userIds, statusList, now);
//            logger.debug("task found for 'In work' and 'Not started' status = {}", tasks.size());
//
//            for (Task task : tasks) {
//                LocalDateTime dueDateTime = Instant.ofEpochMilli(task.getDueDate())
//                        .atZone(ZoneId.of("America/New_York")).toLocalDateTime();
//
//                logger.debug("task found for 'In work' and 'Not started' status with INFO: ID={}, Name='{}', DueDate={}",
//                        task.getTaskId(), task.getTaskName(), dueDateTime);
//            }
//
//            List<Task> overdueTasks = tasks.stream()
//                    .filter(task -> task.getDueDate() <= now)
//                    .collect(Collectors.toList());
//
//            // Log 'now' in readable EST for validation
//            logger.debug("Now (EST) = {}",
//                    Instant.ofEpochMilli(now).atZone(ZoneId.of("America/New_York")).toLocalDateTime());
//
//            logger.debug("task found who passed the due date are = {}", overdueTasks.size());
//
//            for (Task task : overdueTasks) {
//                LocalDateTime dueDateTime = Instant.ofEpochMilli(task.getDueDate())
//                        .atZone(ZoneId.of("America/New_York")).toLocalDateTime();
//
//                logger.debug("Task who passed due date are: ID={}, Name='{}', DueDate={}",
//                        task.getTaskId(), task.getTaskName(), dueDateTime);
//            }
//
//            if (overdueTasks.isEmpty()) {
//                logger.info("ESTTaskMove: No eligible overdue tasks to update, scheduler finished.");
//                return;
//            }
//
//            long newDueDateMillis = ZonedDateTime.now(ZoneId.of("America/New_York"))
//                    .withHour(6).withMinute(0).withSecond(0).withNano(0)
//                    .toInstant().toEpochMilli();
//
//            LocalDateTime newDueDateReadable = Instant.ofEpochMilli(newDueDateMillis)
//                    .atZone(ZoneId.of("America/New_York")).toLocalDateTime();
//
//            for (Task t : overdueTasks) {
//                LocalDateTime currentDueDateReadable = Instant.ofEpochMilli(t.getDueDate())
//                        .atZone(ZoneId.of("America/New_York")).toLocalDateTime();
//                logger.info("Moving taskId={}, taskName='{}', currentDueDate='{}', to newDueDate='{}'",
//                        t.getTaskId(), t.getTaskName(), currentDueDateReadable, newDueDateReadable);
//            }
//
//            List<Long> taskIds = overdueTasks.stream().map(Task::getTaskId).collect(Collectors.toList());
//            String updateStatus = taskService.updateMultipleTasksDueDates(taskIds, newDueDateMillis);
//
//            logger.info("Successfully passed {} task(s) for the next day at 06:00 EST", taskIds.size());
//        } catch (Exception e) {
//            logger.error("ESTTaskMove: An error occurred during task movement: {}", e.getMessage(), e);
//        } finally {
//            ZonedDateTime end = ZonedDateTime.now(ZoneId.of("America/New_York"));
//            logger.info("ESTTaskMove: Scheduler finished at {}", end);
//        }
//    }
//
//}

