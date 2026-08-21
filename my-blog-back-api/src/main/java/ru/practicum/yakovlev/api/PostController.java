package ru.practicum.yakovlev.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.PostRequestDto;

@RestController
@RequestMapping(ApiConstants.POSTS_BASE_PATH)
public interface PostController {

    @GetMapping
    PageResponse getPosts(@RequestParam("search") String searchRequest,
                          @RequestParam("pageNumber") @Min(value = 1, message = "Page number must be greater than zero") Integer page,
                          @RequestParam("pageSize") @Min(value = 1, message = "Page size must be greater than zero") Integer size);

    @PostMapping
    FullPostResponseDto createPost(@RequestBody @Valid PostRequestDto postRequestDto);

    @PutMapping("/{id}")
    FullPostResponseDto updatePost(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid PostRequestDto postRequestDto);

    @GetMapping("/{id}")
    FullPostResponseDto getPost(@PathVariable("id") @Positive Long id);

    @DeleteMapping("/{id}")
    void deletePost(@PathVariable("id") @Positive Long id);

    @PostMapping("/{id}/likes")
    Long likePost(@PathVariable("id") @Positive Long id);

    @PutMapping("/{id}/image")
    void postImage(@PathVariable("id") @Positive Long id, @RequestParam("image") MultipartFile image);

    @GetMapping("/{id}/image")
    byte[] getPostImage(@PathVariable("id") @Positive Long id);

}
