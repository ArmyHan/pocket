package org.homo.controller;

import org.homo.Application;
import org.homo.authority.model.User;
import org.homo.common.config.HomoConfig;
import org.homo.common.constant.OperateTypes;
import org.homo.demo.model.Order;
import org.homo.common.repository.AbstractRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author wujianchuan 2018/12/26
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class OrderRepositoryImplTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    AbstractRepository<Order> repository;

    @Autowired
    ControllerFactory controllerFactory;

    @Autowired
    HomoConfig homoConfig;

    private MockMvc mockMvc;

    @Before
    public void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void test1() {
        Order order = Order.newInstance("ABC-001", new BigDecimal("12.593"));
        order.setUuid("sl92mm34j4mndkj4nmd");
        User user = User.newInstance("Home", "霍姆");
        int effect = repository.getProxy().update(order, user);
        System.out.println("影响行数：" + effect);
    }

    @Test
    public void test2() {
        System.out.println(OperateTypes.SAVE);
    }

    @Test
    public void test3() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/detail"))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void test4() {
        System.out.println(homoConfig.getDescribe());
    }

    @Test
    public void test5() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/save"))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void test6() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/discount"))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}