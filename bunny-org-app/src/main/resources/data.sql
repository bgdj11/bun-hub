-- Novi Sad
INSERT INTO locations (id, name, country, city, address, number, latitude, longitude)
VALUES
    (1, 'Veterinar NS Centar', 'Serbia', 'Novi Sad', 'Bulevar Oslobođenja', 56, 45.2671, 19.8335)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO locations (id, name, country, city, address, number, latitude, longitude)
VALUES
    (2, 'Azil Novi Sad', 'Serbia', 'Novi Sad', 'Futoški put', 23, 45.2510, 19.8369)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO locations (id, name, country, city, address, number, latitude, longitude)
VALUES
    (3, 'Veterinarska ambulanta Liman', 'Serbia', 'Novi Sad', 'Narodnog fronta', 12, 45.2425, 19.8520)
    ON CONFLICT (id) DO NOTHING;

-- Beograd
INSERT INTO locations (id, name, country, city, address, number, latitude, longitude)
VALUES
    (4, 'Veterinar Vračar', 'Serbia', 'Beograd', 'Kneza Miloša', 14, 44.8050, 20.4651)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO locations (id, name, country, city, address, number, latitude, longitude)
VALUES
    (5, 'Azil Beograd Zemun', 'Serbia', 'Beograd', 'Zemunski put', 88, 44.8435, 20.3840)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO locations (id, name, country, city, address, number, latitude, longitude)
VALUES
    (6, 'Veterinarska klinika Novi Beograd', 'Serbia', 'Beograd', 'Bulevar Zorana Đinđića', 101, 44.8156, 20.4361)
    ON CONFLICT (id) DO NOTHING;
