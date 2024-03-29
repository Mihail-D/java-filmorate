package ru.yandex.practicum.filmorate.storage.db;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Component
@Slf4j
@Primary
@AllArgsConstructor
public class FilmDbStorage implements FilmStorage {

    public static final String FILMS = "films";
    public static final String GENRE_FILM = "genre_film";
    public static final String MPA_RATING = "mpa_rating";
    public static final String GENRES = "genres";
    public static final String LIKES = "likes";
    private final JdbcTemplate jdbcTemplate;
    private final DirectorDbStorage directorDbStorage;
    private final MpaDbStorage mpaDbStorage;
    private final GenreStorage genreStorage;


    @Override
    public Film createFilm(Film film) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", film.getName());
        values.put("description", film.getDescription());
        values.put("release_date", film.getReleaseDate());
        values.put("duration", film.getDuration());
        values.put(MPA_RATING + "_id", film.getMpa().getId());

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(FILMS)
                .usingGeneratedKeyColumns("film_id");

        Long filmId = simpleJdbcInsert.executeAndReturnKey(values).longValue();

        addGenres(filmId, film.getGenres());

        if (film.getDirectors().size() != 0) {
            directorDbStorage.addFilm(film.getDirectors(), filmId);
        }

        return getFilmById(filmId);
    }

    private void addGenres(Long filmId, List<Genre> genres) {

        List<Genre> currentFilmGenres = getGenresForFilm(filmId);
        List<Long> addedGenreIds = new ArrayList<>(genres.size());

        for (Genre filmGenre : currentFilmGenres) {
            addedGenreIds.add(filmGenre.getId());
        }

        for (Genre genre : genres) {
            if (addedGenreIds.contains(genre.getId())) {
                continue;
            }

            Map<String, Object> values = new HashMap<>();
            values.put("film_id", filmId);
            values.put("genre_id", genre.getId());

            SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName(GENRE_FILM);

            simpleJdbcInsert.execute(values);

            addedGenreIds.add(genre.getId());
        }
    }

    @Override
    public Film updateFilm(Film film) {
        if (getFilmById(film.getId()) == null) {
            throw new NotFoundException("Фильм с идентификатором " + film.getId() + " не найден.");
        }


        jdbcTemplate.update(
                "update " + FILMS + " set " +
                        " name = ?," +
                        " description = ?," +
                        " release_date = ?," +
                        " duration = ?," +
                        " " + MPA_RATING + "_id = ? " +
                        "where film_id = ?",
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        deleteGenres(film.getId());
        addGenres(film.getId(), film.getGenres());

        if (film.getDirectors().size() == 0) {
            directorDbStorage.deleteFilm(film.getId());
        } else {
            directorDbStorage.addFilm(film.getDirectors(), film.getId());
        }

        return getFilmById(film.getId());
    }

    private void deleteGenres(Long filmId) {
        String sql = "DELETE FROM " + GENRE_FILM + " WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    @Override
    public List<Film> getAllFilms() {

        SqlRowSet filmRows = jdbcTemplate.queryForRowSet("select f.*, mr.name mpa_name, mr.description mpa_description" +
                " from " + FILMS + " as f " +
                "join " + MPA_RATING + " as mr  on mr.mpa_rating_id  = f.mpa_rating_id ");

        ArrayList<Film> films = new ArrayList<>();
        while (filmRows.next()) {
            Mpa mpa = new Mpa();
            Film film = new Film();
            film.setId(filmRows.getLong("film_id"));
            film.setName(filmRows.getString("name"));
            film.setDescription(filmRows.getString("description"));
            Date releaseDateField = filmRows.getDate("release_date");
            film.setReleaseDate(releaseDateField != null ? releaseDateField.toLocalDate() : null);
            film.setDuration(filmRows.getInt("duration"));
            film.setGenres(getGenresForFilm(film.getId()));
            film.setDirectors(directorDbStorage.getDirectorsByFilm(film.getId()));
            mpa.setId(filmRows.getInt(MPA_RATING + "_id"));
            mpa.setName(filmRows.getString("MPA_NAME"));
            mpa.setDescription(filmRows.getString("MPA_DESCRIPTION"));
            film.setMpa(mpa);
            films.add(film);

        }

        return films;
    }

    @Override
    public Film getFilmById(Long filmId) {
        SqlRowSet filmRows = jdbcTemplate.queryForRowSet("select f.*, mr.name mpa_name, mr.description mpa_description " +
                "from " + FILMS + " as f" +
                "  join " + MPA_RATING + " as mr " +
                " on mr." + MPA_RATING + "_id  = f.mpa_rating_id where f.film_id = ?", filmId);
        Mpa mpa = new Mpa();
        if (filmRows.next()) {

            Film film = new Film();
            film.setId(filmRows.getLong("film_id"));
            film.setName(filmRows.getString("name"));
            film.setDescription(filmRows.getString("description"));
            Date releaseDateField = filmRows.getDate("release_date");
            film.setReleaseDate(releaseDateField != null ? releaseDateField.toLocalDate() : null);
            film.setDuration(filmRows.getInt("duration"));
            film.setGenres(getGenresForFilm(film.getId()));
            mpa.setId(filmRows.getInt(MPA_RATING + "_id"));
            mpa.setName(filmRows.getString("MPA_NAME"));
            mpa.setDescription(filmRows.getString("MPA_DESCRIPTION"));
            film.setDirectors(directorDbStorage.getDirectorsByFilm(filmId));
            film.setMpa(mpa);

            log.info("Найден фильм: {} {}", film.getId(), film.getName());

            return film;
        } else {
            log.info("Пользователь с идентификатором {} не найден.", filmId);
            return null;
        }
    }

    public List<Genre> getGenresForFilm(Long filmId) {

        String sql = "select * " +
                "from " + GENRES + " as g " +
                "join " + GENRE_FILM + " as gf on gf.genre_id = g.genre_id " +
                "where gf.film_id = ?";
        SqlRowSet genreRows = jdbcTemplate.queryForRowSet(sql, filmId);
        ArrayList<Genre> genres = new ArrayList<>();
        while (genreRows.next()) {
            Genre genre = new Genre();
            genre.setId(genreRows.getLong("genre_id"));
            genre.setName(genreRows.getString("name"));

            genres.add(genre);
        }
        return genres;

    }

    @Override
    public void addLike(Long filmId, Long userId) {

        Map<String, Object> values = new HashMap<>();
        values.put("film_id", filmId);
        values.put("user_id", userId);


        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(LIKES);
        simpleJdbcInsert.execute(values);
    }

    @Override
    public List<Film> getMostPopularFilms(Integer limit, Integer genreId, Integer year) {
        SqlRowSet likesRows;
        ArrayList<Film> mostPopularFilms = new ArrayList<>();

        if (genreId == null && year == null) {
            String sql = "SELECT f.film_id " +
                    "FROM " + FILMS + " AS f " +
                    "LEFT OUTER JOIN " + LIKES + " AS l ON l.film_id = f.film_id " +
                    "GROUP BY f.film_id " +
                    "ORDER BY COUNT(l.film_id) DESC " +
                    "LIMIT ?";
            likesRows = jdbcTemplate.queryForRowSet(sql, limit);

        } else if (genreId != null && year != null) {
            String sql = "SELECT f.film_id " +
                    "FROM " + FILMS + " AS f " +
                    "LEFT OUTER JOIN " + LIKES + " AS l ON l.film_id = f.film_id " +
                    "LEFT JOIN  " + GENRE_FILM + " AS gf ON f.film_id = gf.film_id " +
                    "WHERE  gf.genre_id = ? AND year(f.release_date) = ? " +
                    "limit ?";
            likesRows = jdbcTemplate.queryForRowSet(sql, genreId, year, limit);

        } else if (year == null) {
            String sql = "SELECT f.film_id " +
                    "FROM " + FILMS + " AS f " +
                    "LEFT OUTER JOIN " + LIKES + " AS l ON l.film_id = f.film_id " +
                    "LEFT JOIN " + GENRE_FILM + " AS gf ON f.film_id = gf.film_id " +
                    "WHERE gf.genre_id = ? " +
                    "GROUP BY f.film_id " +
                    "limit ?";
            likesRows = jdbcTemplate.queryForRowSet(sql, genreId, limit);

        } else {
            String sql = "SELECT f.film_id " +
                    "FROM " + FILMS + " AS f " +
                    "LEFT OUTER JOIN " + LIKES + " AS l ON l.film_id = f.film_id " +
                    "LEFT JOIN " + GENRE_FILM + " AS gf ON f.film_id = gf.film_id " +
                    "WHERE year(f.release_date) = ? " +
                    "GROUP BY f.film_id " +
                    "limit ?";
            likesRows = jdbcTemplate.queryForRowSet(sql, year, limit);
        }
        while (likesRows.next()) {
            Long filmId = likesRows.getLong("film_id");
            mostPopularFilms.add(getFilmById(filmId));
        }
        return mostPopularFilms;
    }

    @Override
    public List<Film> getCommonFilms(Integer userId, Integer friendId) {
        SqlRowSet likesRows;
        ArrayList<Film> commonFilms = new ArrayList<>();

        String sql = "SELECT f.film_id " +
                "FROM " + FILMS + " AS f " +
                "LEFT OUTER JOIN " + LIKES + " AS l ON f.film_id = l.film_id " +
                "WHERE f.film_id IN ( " +
                "   SELECT f.film_id " +
                "   FROM " + FILMS + " AS f " +
                "   LEFT JOIN " + LIKES + " AS l ON f.film_id = l.film_id " +
                "   WHERE user_id = ? " +
                ") " +
                "AND user_id = ? " +
                "GROUP BY f.film_id " +
                "ORDER BY count(DISTINCT user_id) DESC ";
        likesRows = jdbcTemplate.queryForRowSet(sql, userId, friendId);

        while (likesRows.next()) {
            Long filmId = likesRows.getLong("film_id");
            commonFilms.add(getFilmById(filmId));
        }
        return commonFilms;
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM " + LIKES + " WHERE film_id = ? AND user_id = ? ";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> findFilmsByDirector(Long directorId, Optional<String> sortBy) {
        log.info("Получение фильмов режиссера с id = {}", directorId);

        StringJoiner sqlQuery = new StringJoiner(" ");

        sqlQuery.add(
                "SELECT f.*, count(l.user_id) AS " + LIKES + ", EXTRACT(YEAR FROM (f.release_date)) AS sort_by_year " +
                        "FROM " + FILMS + " AS f " +
                        "LEFT OUTER JOIN " + FILMS + "_director AS fd ON f.film_id = fd.film_id " +
                        "LEFT OUTER JOIN " + LIKES + " AS l ON l.film_id = f.film_id " +
                        "WHERE fd.director_id = " + directorId + " " +
                        "GROUP BY f.film_id"
        );

        switch (sortBy.orElse("")) {
            case "like": {
                sqlQuery.add("ORDER BY sort_by_like ASC");
                break;
            }
            case "year": {
                sqlQuery.add("ORDER BY sort_by_year ASC");
                break;
            }
        }


        List<Film> films = mapToFilms(
                jdbcTemplate.queryForList(sqlQuery.toString())
        );

        if (films.size() == 0) {
            throw new NotFoundException("Фильма режиссера не найдены");
        }

        return films;
    }

    private List<Film> mapToFilms(List<Map<String, Object>> allFilms) {
        ArrayList<Film> films = new ArrayList<>();

        allFilms.forEach(f -> {
            Long id = Long.parseLong(f.get("film_id").toString());

            films.add(
                    Film
                            .builder()
                            .id(id)
                            .description(f.get("description").toString())
                            .name(f.get("name").toString())
                            .duration((int) f.get("duration"))
                            .releaseDate(LocalDate.parse(f.get("release_date").toString()))
                            .directors(directorDbStorage.getDirectorsByFilm(id))
                            .mpa(mpaDbStorage.getMpaById((int) f.get(MPA_RATING + "_id")))
                            .genres(getGenresForFilm(id))
                            .build()
            );
        });

        return films;
    }

    @Override
    public List<Film> getRecommendations(Integer userId) {
        return jdbcTemplate.query(RECOMMENDED_FILMS, new Object[]{userId, userId, userId}, (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("film_name"));
            film.setDescription(rs.getString("film_description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            film.setMpa(mpaDbStorage.getMpaById(rs.getInt(MPA_RATING + "_id")));
            String genresStr = rs.getString(GENRES);
            List<Genre> genres = new ArrayList<>();
            if (genresStr != null) {
                String[] genreNames = genresStr.split(", ");
                for (String genreName : genreNames) {
                    Long genreId = genreStorage.getGenreIdByName(genreName);
                    if (genreId == null) {
                        genreId = genreStorage.addGenre(new Genre(null, genreName));
                    }
                    genres.add(new Genre(genreId, genreName));
                }
            }
            film.setGenres(genres);
            return film;
        });
    }

    public List<Film> searchBy(String query, String by) {
        List<Film> searchResults = new ArrayList<>();

        switch (by) {
            case "title": {
                String sql = "SELECT " + FILMS + ".*, G.genre_id, GT.name AS genre_name, COUNT(l.film_id) as count " +
                        "FROM " + FILMS + " " +
                        "LEFT JOIN " + GENRE_FILM + " g ON " + FILMS + ".film_id = g.film_id " +
                        "LEFT JOIN " + GENRES + " gt on g.genre_id = gt.genre_id " +
                        "LEFT JOIN " + LIKES + " l ON " + FILMS + ".film_id=l.film_id " +
                        "WHERE LOWER(" + FILMS + ".name) LIKE LOWER(CONCAT('%',?,'%')) " +
                        "GROUP BY " + FILMS + ".film_id " +
                        "ORDER BY count DESC";
                searchResults = jdbcTemplate.query(sql, this::mapToFilm, query);
                break;
            }
            case "director": {
                String sql = "SELECT " + FILMS + ".*, COUNT(l.film_id) as count " +
                        "FROM " + FILMS + " " +
                        "JOIN " + FILMS + "_director  df ON films.film_id=df.film_id " +
                        "JOIN directors  d ON df.director_id=d.director_id " +
                        "LEFT JOIN " + LIKES + " l ON " + FILMS + ".film_id=l.film_id " +
                        "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%',?,'%')) " +
                        "GROUP BY " + FILMS + ".film_id " +
                        "ORDER BY count DESC";
                searchResults = jdbcTemplate.query(sql, this::mapToFilm, query);
                break;
            }
            case "title,director": {
                String sql = "SELECT " + FILMS + ".*, COUNT(l.film_id) as count " +
                        "FROM " + FILMS + " " +
                        "LEFT JOIN " + FILMS + "_director  df ON films.film_id=df.film_id " +
                        "LEFT JOIN directors  d ON df.director_id=d.director_id " +
                        "LEFT JOIN " + LIKES + " l ON " + FILMS + ".film_id=l.film_id " +
                        "WHERE LOWER(" + FILMS + ".name) LIKE LOWER(CONCAT('%',?,'%')) " +
                        "OR LOWER(d.name) LIKE LOWER(CONCAT('%',?,'%')) " +
                        "GROUP BY " + FILMS + ".film_id " +
                        "ORDER BY count DESC";
                searchResults = jdbcTemplate.query(sql, this::mapToFilm, query, query);
                break;
            }
        }
        return searchResults;
    }

    @Override
    public void deleteFilm(Long id) {
        String sqlQuery = "DELETE FROM " + FILMS + " WHERE film_id = ?";
        jdbcTemplate.update(sqlQuery, id);
    }

    private Film mapToFilm(ResultSet rs, int rowNum) throws SQLException {
        Long id = rs.getLong("film_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        Integer duration = rs.getInt("duration");
        LocalDate releaseDate = rs.getDate("release_date").toLocalDate();
        Mpa mpa = mpaDbStorage.getMpaById(rs.getInt(MPA_RATING + "_id"));
        List<Genre> genres = getGenresForFilm(id);
        LinkedHashSet<Director> directors = directorDbStorage.getDirectorsByFilm(id);

        log.info("Создание объекта фильма из базы с id {}", id);

        return Film.builder()
                .id(id)
                .name(name)
                .description(description)
                .duration(duration)
                .releaseDate(releaseDate)
                .mpa(mpa)
                .genres(genres)
                .directors(directors)
                .build();
    }
}
