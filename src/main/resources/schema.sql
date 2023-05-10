CREATE table film(film_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                  name varchar(255)  NOT NULL ,
                  description varchar(255) NOT NULL ,
                  release_date date NOT NULL ,
                  duration integer NOT NULL ,
                  genre varchar(255) REFERENCES genre(genre_id),
                  mpa_rating varchar(255))-- Напишите здесь свой код

CREATE table user(user_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                  name varchar(255)  NOT NULL ,
                  email varchar(255) ,
                  login varchar NOT NULL ,
                  birthday date NOT NULL

CREATE table genre(genre_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                  name varchar(255)  NOT NULL)

CREATE table likes(user_id integer REFERENCES user(user_id),
                   film_id integer REFERENCES  film(film_id))

CREATE table friends(friends_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                     user_id integer  REFERENCES user(user_id),
                     friendship integer REFERENCES  status_frendship (status_frendship_id))

CREATE table status_frendship(status_frendship_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                              status varchar(255))





