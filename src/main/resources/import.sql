-- Insert establishments
INSERT INTO establishments (id, name, address, phone, email, created_at, updated_at) VALUES
  (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'Foot Arena Lyon', '12 rue du sport, Lyon', '0102030405', 'lyon@footarena.com', '2021-06-01 12:00:00', '2021-06-01 12:00:00'),
  (UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 'Foot Arena Paris', '34 avenue du ballon, Paris', '0203040506', 'paris@footarena.com', '2021-06-01 12:00:00', '2021-06-01 12:00:00'),
  (UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), 'Foot Arena Marseille', '56 route des stades, Marseille', '0304050607', 'marseille@footarena.com', '2021-06-01 12:00:00', '2021-06-01 12:00:00');

-- Insert fields
INSERT INTO fields (id, name, location, surface_type, capacity, available, establishment_id) VALUES
  (UUID_TO_BIN('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), 'Terrain 1', 'Lyon - Nord', 'Gazon synthétique', 10, b'1', UUID_TO_BIN('11111111-1111-1111-1111-111111111111')),
  (UUID_TO_BIN('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'Terrain 2', 'Lyon - Sud', 'Gazon naturel', 12, b'0', UUID_TO_BIN('11111111-1111-1111-1111-111111111111')),
  (UUID_TO_BIN('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1'), 'Terrain 3', 'Paris - Est', 'Gazon synthétique', 8, b'1', UUID_TO_BIN('22222222-2222-2222-2222-222222222222')),
  (UUID_TO_BIN('ccccccc1-cccc-cccc-cccc-ccccccccccc1'), 'Terrain 4', 'Marseille - Vieux-Port', 'Gazon naturel', 14, b'1', UUID_TO_BIN('33333333-3333-3333-3333-333333333333')),
  (UUID_TO_BIN('ccccccc2-cccc-cccc-cccc-ccccccccccc2'), 'Terrain 5', 'Marseille - Est', 'Gazon synthétique', 6, b'0', UUID_TO_BIN('33333333-3333-3333-3333-333333333333'));

-- Insert users
INSERT INTO users (id, first_name, last_name, email, password, enabled, role) VALUES
  (UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), 'Alice', 'Admin', 'alice.admin@footarena.com', '123456789', true, 'ADMIN'),
  (UUID_TO_BIN('55555555-5555-5555-5555-555555555555'), 'Bob', 'Manager', 'bob.manager@footarena.com', '123456789', true, 'MANAGER'),
  (UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), 'Charlie', 'Player', 'charlie.player@footarena.com', '123456789', true, 'PLAYER');
