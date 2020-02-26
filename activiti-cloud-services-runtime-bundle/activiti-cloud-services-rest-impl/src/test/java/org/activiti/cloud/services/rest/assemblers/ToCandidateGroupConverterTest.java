package org.activiti.cloud.services.rest.assemblers;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ToCandidateGroupConverterTest {

    private ToCandidateGroupConverter toCandidateGroupConverter;

    @Before
    public void setUp(){
        toCandidateGroupConverter = new ToCandidateGroupConverter();
    }

    @Test
    public void shouldConvertStringGroupsToCanidateGroups(){
        //given
        String group = "group1";
        List<String> groupList = new ArrayList<>();
        groupList.add(group);
        //when
        List<CandidateGroup> convertedGroupList = toCandidateGroupConverter.from(groupList);
        //then
        assertThat(convertedGroupList.get(0)).isInstanceOf(CandidateGroup.class);
        assertThat(convertedGroupList.get(0).getGroup()).isEqualTo(group);
    }

}
