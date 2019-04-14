package application.slide.infrastructure;

import application.slide.domain.RandomSlide;
import application.slide.domain.Slide;
import io.vavr.collection.*;
import lombok.RequiredArgsConstructor;

import java.util.Random;

@RequiredArgsConstructor
public class SlideRepository {
    private final Random random;

    private volatile Set<RandomSlide> slides = HashSet.empty();
    private volatile Map<Slide, RandomSlide> slideIndex = HashMap.empty();
    private volatile Map<Integer, List<RandomSlide>> sizeIndex = HashMap.empty();

    public List<RandomSlide> findAllBySize(int slideSize) {
        return sizeIndex.get(slideSize).getOrElse(List.empty());
    }

    public boolean exists(Slide slide) {
        return slideIndex.containsKey(slide);
    }

    public synchronized Set<RandomSlide> saveAll(List<Slide> slides) {
        List<RandomSlide> randomSlides = slides.map(slide -> new RandomSlide(slide, generateRandom()));
        this.slides = this.slides.addAll(randomSlides);
        this.sizeIndex = this.sizeIndex.merge(randomSlides.groupBy(randomSlide -> randomSlide.getSlide().size()), List::appendAll);
        this.slideIndex = this.slideIndex.merge(randomSlides.toMap(RandomSlide::getSlide, randomSlide -> randomSlide));
        return randomSlides.toSet();
    }

    private int generateRandom() {
        return random.nextInt(12) - 1;
    }
}
