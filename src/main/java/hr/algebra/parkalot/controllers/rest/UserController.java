package hr.algebra.parkalot.controllers.rest;


import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.dtos.UserDto;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.services.db.UserService;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> getUsers(){
        return HttpUtils.produceOkResponseWithBody(userService
                .findAll().stream().map(UserDto::new).toList());
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateUser(@RequestBody UserDto userDto) {
        return HttpUtils.produceOkResponseWithBody(userService.updateUser(userDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if(!userService.existsById(id)){
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.USER_NOT_FOUND, id);
        }
        userService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}
