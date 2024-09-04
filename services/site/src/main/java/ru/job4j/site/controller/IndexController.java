package ru.job4j.site.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.job4j.site.dto.*;
import ru.job4j.site.service.*;

import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.job4j.site.controller.RequestResponseTools.getToken;

@Controller
@AllArgsConstructor
@Slf4j
public class IndexController {
    private final CategoriesService categoriesService;
    private final InterviewsService interviewsService;
    private final AuthService authService;
    private final ProfilesService profilesService;
    private final NotificationService notifications;
    private final TopicsService topicsService;

    @GetMapping({"/", "index"})
    public String getIndexPage(Model model, HttpServletRequest req) throws JsonProcessingException {
        RequestResponseTools.addAttrBreadcrumbs(model,
                "Главная", "/"
        );
        try {
            List<CategoryDTO> categories = categoriesService.getMostPopular();
            model.addAttribute("categories", categories);
            List<Integer> newInterviews = new ArrayList<>(categories.size());
            Map<Integer, List<TopicIdNameDTO>> topicsByCategoriesId = topicsService.getTopicIdsNameDtoByCategories(
                    categories.stream().map(CategoryDTO::getId).collect(Collectors.toList()));

            for (CategoryDTO category : categories) {
                int newCount = Math.toIntExact(interviewsService.getByTopicsIds(
                        topicsByCategoriesId.get(category.getId()).stream()
                                .map(TopicIdNameDTO::getId).collect(Collectors.toList()), 0, 1000)
                        .stream().filter(x -> LocalDateTime.parse(x.getCreateDate(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                .isAfter(LocalDateTime.now().minusDays(3))).count());
                newInterviews.add(newCount);
            }
            model.addAttribute("categories_newCount", newInterviews);
            var token = getToken(req);
            if (token != null) {
                var userInfo = authService.userInfo(token);
                model.addAttribute("userInfo", userInfo);
                model.addAttribute("userDTO", notifications.findCategoriesByUserId(userInfo.getId()));
                RequestResponseTools.addAttrCanManage(model, userInfo);
            }
        } catch (Exception e) {
            log.error("Remote application not responding. Error: {}. {}, ", e.getCause(), e.getMessage());
        }
        var interviews = interviewsService.getByType(1);
        List<ProfileDTO> profiles = new ArrayList<>();
        interviews.forEach(x -> {
            var profile = profilesService.getProfileById(x.getSubmitterId());
            if (profile.isPresent()) {
                profiles.add(profile.get());
            } else {
                profiles.add(new ProfileDTO());
            }
        });
        model.addAttribute("new_interviews", interviews);
        model.addAttribute("profiles", profiles);
        return "index";
    }
}