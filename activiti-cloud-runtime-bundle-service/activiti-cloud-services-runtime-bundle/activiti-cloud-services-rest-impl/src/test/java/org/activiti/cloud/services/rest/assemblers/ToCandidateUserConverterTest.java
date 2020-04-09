package org.activiti.cloud.services.rest.assemblers;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


public class ToCandidateUserConverterTest {

    private ToCandidateUserConverter toCandidateUserConverter;

    @BeforeEach
    public void setUp(){
        toCandidateUserConverter = new ToCandidateUserConverter();
    }

    @Test
    public void shouldConvertStringUsersToCanidateUsers(){
        //given
        String user = "user1";
        List<String> userList = new ArrayList<>();
        userList.add(user);
        //when
        List<CandidateUser> convertedUserList = toCandidateUserConverter.from(userList);
        //then
        assertThat(convertedUserList.get(0)).isInstanceOf(CandidateUser.class);
        assertThat(convertedUserList.get(0).getUser()).isEqualTo(user);
    }

}
