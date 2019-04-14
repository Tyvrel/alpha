package application.slide.domain;

import application.slide.infrastructure.SlideRepository;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

class SlideServiceTest {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);
    private final Random random = new Random(123);

    @Test
    void slideExistsShouldCorrectlyCheckSlideExistence() {
        List<Slide> persistedSlides = List.of(
                new Slide("Mary"),
                new Slide("Mary", "gone"),
                new Slide("Mary's", "gone"),
                new Slide("went", "Mary's"),
                new Slide("went")
        );

        SlideRepository slideRepository = new SlideRepository(new Random(123));
        SentenceSplittingServiceFactory sentenceSplittingServiceFactory = new SentenceSplittingServiceFactory();
        Set<RandomSlide> randomSlides = slideRepository.saveAll(persistedSlides);
        SlideService slideService = new SlideService(slideRepository, sentenceSplittingServiceFactory, EXECUTOR_SERVICE);
        Assertions.assertTrue(slideService.slideExists("Mary"));
        Assertions.assertTrue(slideService.slideExists("Mary gone"));
        Assertions.assertTrue(slideService.slideExists("Mary's gone"));
        Assertions.assertTrue(slideService.slideExists("went Mary's"));
        Assertions.assertTrue(slideService.slideExists("Mary went"));
        Assertions.assertTrue(slideService.slideExists("Mary's went"));
        Assertions.assertFalse(slideService.slideExists("gone Mary's"));
        Assertions.assertTrue(slideService.slideExists("Mary went Mary's gone"));
        Assertions.assertFalse(slideService.slideExists("Peter gone Peter's gone"));
    }


    @ParameterizedTest
    @MethodSource("getFindSlidesShouldReturnCorrectSlidesParameters")
    void findSlidesShouldReturnCorrectSlides(String sentence, List<Slide> persistedSlides, List<Slide> expectedSlides) {
        SlideRepository slideRepository = new SlideRepository(new Random(123));
        SentenceSplittingServiceFactory sentenceSplittingServiceFactory = new SentenceSplittingServiceFactory();
        Set<RandomSlide> savedSlides = slideRepository.saveAll(persistedSlides);
        SlideService slideService = new SlideService(slideRepository, sentenceSplittingServiceFactory, EXECUTOR_SERVICE);
        List<RandomSlide> foundSlides = slideService.findSlides(sentence);
        List<RandomSlide> expectedRandomSlides = expectedSlides.map(slide -> savedSlides
                .filter(randomSlide -> Objects.equals(randomSlide.getSlide(), slide)).head());

        MatcherAssert.assertThat(foundSlides, Matchers.containsInAnyOrder(expectedRandomSlides.toJavaArray(RandomSlide[]::new)));
    }

    private static Stream<Arguments> getFindSlidesShouldReturnCorrectSlidesParameters() {
        return Stream.of(
                Arguments.of(
                        "Mary went Mary's gone",
                        List.of("Mary", "Mary gone", "Mary's gone", "went Mary's", "went").map(SlideServiceTest::toSlide),
                        List.of("went Mary's", "Mary").map(SlideServiceTest::toSlide)
                ),
                Arguments.of(
                        "Mary gone Mary gone Mary gone",
                        List.of("Mary", "Mary gone", "Mary's gone", "went Mary's", "went").map(SlideServiceTest::toSlide),
                        List.of("Mary gone", "Mary gone", "Mary gone").map(SlideServiceTest::toSlide)
                ),
                Arguments.of(
                        "Mary gone Mary gone Mary",
                        List.of("Mary gone Mary", "Mary gone").map(SlideServiceTest::toSlide),
                        List.of("Mary gone Mary").map(SlideServiceTest::toSlide)
                ),
                Arguments.of(
                        "Mary gone Mary gone Mary's",
                        List.of("gone Mary's").map(SlideServiceTest::toSlide),
                        List.of("gone Mary's").map(SlideServiceTest::toSlide)
                ),
                Arguments.of(
                        "Mary gone Mary gone Mary",
                        List.<String>empty().map(SlideServiceTest::toSlide),
                        List.<String>empty().map(SlideServiceTest::toSlide)
                )
        );
    }

    private static Slide toSlide(String string) {
        return new Slide(string.split(" "));
    }

}
