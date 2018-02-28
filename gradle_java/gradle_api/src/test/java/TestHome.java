import org.apache.shiro.crypto.hash.Md5Hash;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Cheng Yufei
 * @create 2017-12-17 上午11:15
 **/

public class TestHome {


    @Test
    public void md5() {
        String str = "123456";
        Md5Hash md5Hash = Md5Hash.fromHexString(str);
        String hex = md5Hash.toHex();
        System.out.println(hex);
    }

    @Test
    public void stream() throws IOException {
        List<String> list = Lists.newArrayList("cheng", "yu", "fei", "yu");
        System.out.println("-----------------原始数据------------------");
        System.out.println(list);


        //排除重复数据
        List<String> collect = list.stream().distinct().collect(Collectors.toList());
        System.out.println("----------------distinct-------------------");
        System.out.println(collect);


        List<String> collect1 = list.stream().limit(2).collect(Collectors.toList());
        System.out.println("----------------limit-------------------");
        System.out.println(collect1);


        //skip(n): 返回一个忽略前n个的stream
        List<String> collect2 = list.stream().skip(2).collect(Collectors.toList());
        System.out.println("-----------------skip------------------");
        System.out.println(collect2);


        //下标 <2 的元素添加id属性，其余不添加
        List<UserTest> userTests = Lists.newArrayList(new UserTest("a"), new UserTest("b"), new UserTest("c"), new UserTest("d"), new UserTest("e"));
        List<UserTest> userTestsRangeClose = IntStream.rangeClosed(0, userTests.size() - 1).mapToObj(i -> {
            UserTest userTest = userTests.get(i);
            if (i < 2) {
                userTest.setId(i + 1);
            }
            return userTest;
        }).collect(Collectors.toList());

        //range 不包含右面
        List<UserTest> userTestsRange = IntStream.range(0, userTests.size() - 1).mapToObj(i -> {
            UserTest userTest = userTests.get(i);
            if (i < 2) {
                userTest.setId(i + 1);
            }
            return userTest;
        }).collect(Collectors.toList());

        System.out.println("-------------UserTests--------------");
        System.out.println(userTests);
        System.out.println("-------------userTestsRangeClose--------------");
        System.out.println(userTestsRangeClose);
        System.out.println("-------------userTestsRange--------------");
        System.out.println(userTestsRange);

        //文件转 Stream
        List<String> collect3 = Files.lines(Paths.get("/Users/chengyufei/Downloads/b.txt"), Charset.defaultCharset()).collect(Collectors.toList());
//        System.out.println(collect3);

    }
}