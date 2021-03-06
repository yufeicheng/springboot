package cyf.gradle.api.controller;

import cyf.gradle.api.service.CommandOrder;
import cyf.gradle.api.service.CommandUser;
import cyf.gradle.api.service.CommandUserForAnnotation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Cheng Yufei
 * @create 2018-06-04 20:16
 **/
@RestController
@RequestMapping("/command")
@Slf4j
public class CommandController {

    private static ThreadLocal threadLocal;


    @Autowired
    private CommandUserForAnnotation commandUserForAnnotation;

    @GetMapping("/get")
    public String get() {
        CommandOrder order = new CommandOrder("Order");
        CommandOrder order_2 = new CommandOrder("Order_2");
        CommandUser user = new CommandUser("User");
        CommandUser user_2 = new CommandUser("Taylor");
        CommandUser user_3 = new CommandUser("Swift");

        /**
         * execute() 实现run方法同步
         * queue() 实现run 方法的异步执行
         */
       /* String orderExecute = order.execute();
        log.debug("order-execute {}", orderExecute);

        String userExecute = user.execute();
        log.debug("user-execute {}", userExecute);

        String user_2Execute = user_2.execute();
        log.debug("user_2-execute {}", user_2Execute);*/

        Future<String> queue = user_3.queue();

        String order_2Execute = order_2.execute();
        log.debug("order_2-execute {}", order_2Execute);
        return "";
    }

    /**
     * 注解形式实现异步及熔断降级
     *
     * @param username
     * @param orderName
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/user",produces = "application/json;charset=UTF-8")
    public String get_2(@RequestParam String username, @RequestParam String orderName) throws Exception {
        threadLocal = new ThreadLocal();
        threadLocal.set("i am threadLocal");
        log.debug("ThreadLocal:" + threadLocal.get());
        //commandUserForAnnotation.async(username);
               /*String run = commandUserForAnnotation.run(username, threadLocal);
             commandUserForAnnotation.async(orderName, threadLocal);
        */
        //String handle = commandUserForAnnotation.sync(orderName, threadLocal);
        String handle = commandUserForAnnotation.handle(username, threadLocal);
        return handle;
    }
}
