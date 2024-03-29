package ru.yandex.practicum.filmorate.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class UserService {

    private final UserStorage userStorage;
    private final FeedService feedService;
    private final FilmStorage filmStorage;


    public Optional<User> getUserById(Long id) {
        if (userStorage.getUserbyId(id) == null) {
            log.error("Пользователь не может быть равен null");
            throw new NotFoundException("Пользователь не может быть равен null");
        } else {
            return Optional.ofNullable(userStorage.getUserbyId(id));
        }
    }

    public User createUser(User user) {
        validateNewUser(user);
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }
        return userStorage.createUser(user);

    }

    public User updateUser(User user) {
        validateNewUser(user);
        return userStorage.updateUser(user);
    }


    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public void addFriend(Long userId, Long friendId) {

        User user = userStorage.getUserbyId(userId);
        User userFriend = userStorage.getUserbyId(friendId);

        if (user == null) {
            log.error("Не найден пользователь с id = {}", userId);
            throw new NotFoundException("Не найден пользователь с указанным id");
        }
        if (userFriend == null) {
            log.error("Не найден пользователь с id = {}", friendId);
            throw new NotFoundException("Не найден пользователь с указанным id");
        }

        userStorage.createFriend(user, userFriend);
        feedService.addFriend(userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {

        User user = userStorage.getUserbyId(userId);
        User userFriend = userStorage.getUserbyId(friendId);

        if (user == null) {
            log.error("Не найден пользователь с id = {}", userId);
            throw new NotFoundException("Не найден пользователь с указанным id");
        }
        if (userFriend == null) {
            log.error("Не найден пользователь с id = {}", friendId);
            throw new NotFoundException("Не найден пользователь с указанным id");
        }
        userStorage.deleteFriend(userId, friendId);
        feedService.deleteFriend(userId, friendId);

    }

    public List<User> getFriendList(Long userId) {
        validateUser(userId);
        return userStorage.getFriendList(userId);
    }

    public List<User> getMutualFriends(Long id, Long otherId) {
        return userStorage.getMutualFriends(id, otherId);
    }

    public void validateAddFriend(Long userId, Long friendId) throws ValidateException {
        if (userId == null || friendId == null) {
            log.error("Не указаны параметры для добавления в друзья");
            throw new ValidateException("id не может быть равен null");
        }
    }

    public void validateNewUser(User user) throws ValidateException {

        if (user.getName() == null && user.getLogin() == null) {
            log.error("Имя пользователя и логин являются пустыми");
            throw new ValidateException("Имя пользователя и логин являются пустыми");
        }
        if (user.getLogin() == null || user.getLogin().contains(" ")) {
            log.error("Неправильный логин пользователя!");
            throw new ValidateException("Неправильный логин пользователя!");
        }
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            log.error("Неправильный email пользователя");
            throw new ValidateException("Неправильный email пользователя");
        }

        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Неправильная дата рождения пользователя");
            throw new ValidateException("Неправильная дата рождения пользователя");
        }
    }

    public void validateUserFriend(Long id) {
        if (id == null && getUserById(id) == null) {
            log.error("Не указаны параметры для удаления из друзей");
            throw new ValidateException("id не может быть равен null");
        }
    }

    public List<Film> getRecommendations(Integer userId) {
        log.debug("Recommendations for films to watch from user with ID {}", userId);
        return filmStorage.getRecommendations(userId);
    }

    public void validateUser(Long id) {
        User user = getUserById(id).get();
        if (user == null) {
            throw new NotFoundException("Не найден пользователь с указанным id");
        }
    }

    public void deleteUser(Long id) {
        userStorage.deleteUser(id);
    }

}
