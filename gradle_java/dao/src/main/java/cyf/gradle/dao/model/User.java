package cyf.gradle.dao.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable, Cloneable {

    @ApiModelProperty(hidden = true)
    private Integer id;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "密码")
    private String pwd;

    private SysRole sysRole;
    private String excessProperty;


    @ApiModelProperty(hidden = true)
    private List<SysRole> roleList;

    private static final long serialVersionUID = 1L;

    public User(Integer id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", pwd='" + pwd + '\'' +
                '}';
    }

    /**
     * 实现 Cloneable（clone()被protected修饰） 需重写 clone()为public
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public User clone() throws CloneNotSupportedException {
        return (User) super.clone();
    }
}