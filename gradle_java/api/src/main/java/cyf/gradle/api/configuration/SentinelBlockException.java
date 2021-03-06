package cyf.gradle.api.configuration;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import cyf.gradle.dao.model.User;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * @author Cheng Yufei
 * @create 2019-04-11 17:16
 **/
@Slf4j
public class SentinelBlockException {

    public static List<User> blockHandle(String name, BlockException ex){
        log.error("SentinelBlockException>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        return Collections.EMPTY_LIST;
    }
}
