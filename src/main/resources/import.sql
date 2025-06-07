-- Insert establishments
INSERT INTO establishments (id, name, address, phone, email, created_at, updated_at) VALUES (UUID_TO_BIN('569e9fd0-9362-4377-a907-7c480d969409'), 'Foot Arena Lyon', '12 rue du sport, Lyon', '0102030405', 'lyon@footarena.com', '2021-06-01 12:00:00', '2021-06-01 12:00:00');
INSERT INTO establishments (id, name, address, phone, email, created_at, updated_at) VALUES (UUID_TO_BIN('dc1fafc9-92bc-4b1e-8cad-360c532cf5af'), 'Foot Arena Paris', '34 avenue du ballon, Paris', '0203040506', 'paris@footarena.com', '2021-06-01 12:00:00', '2021-06-01 12:00:00');
INSERT INTO establishments (id, name, address, phone, email, created_at, updated_at) VALUES (UUID_TO_BIN('32d80ba0-43f2-4306-a47d-6d70270e1e22'), 'Foot Arena Marseille', '56 route des stades, Marseille', '0304050607', 'marseille@footarena.com', '2021-06-01 12:00:00', '2021-06-01 12:00:00');

-- Insert fields
INSERT INTO fields (id, name, location, surface_type, capacity, available, establishment_id) VALUES (UUID_TO_BIN('6b1d55c9-ee5f-4199-ac71-37d5b02fac6d'), 'Terrain 1', 'Lyon - Nord', 'Gazon synthétique', 10, true, UUID_TO_BIN('569e9fd0-9362-4377-a907-7c480d969409'));
INSERT INTO fields (id, name, location, surface_type, capacity, available, establishment_id) VALUES (UUID_TO_BIN('1d6a5fd2-a16f-4d72-be34-5cd162a224ac'), 'Terrain 2', 'Lyon - Sud', 'Gazon naturel', 12, false, UUID_TO_BIN('569e9fd0-9362-4377-a907-7c480d969409'));
INSERT INTO fields (id, name, location, surface_type, capacity, available, establishment_id) VALUES (UUID_TO_BIN('db31c779-ca73-498b-b2af-b8e620fb154b'), 'Terrain 3', 'Paris - Est', 'Gazon synthétique', 8, true, UUID_TO_BIN('dc1fafc9-92bc-4b1e-8cad-360c532cf5af'));
INSERT INTO fields (id, name, location, surface_type, capacity, available, establishment_id) VALUES (UUID_TO_BIN('2975b682-d952-43f4-8f84-65063d65c4bd'), 'Terrain 4', 'Marseille - Vieux-Port', 'Gazon naturel', 14, true, UUID_TO_BIN('32d80ba0-43f2-4306-a47d-6d70270e1e22'));
INSERT INTO fields (id, name, location, surface_type, capacity, available, establishment_id) VALUES (UUID_TO_BIN('9d142039-84a9-47eb-b8fc-ff8a38033f3e'), 'Terrain 5', 'Marseille - Est', 'Gazon synthétique', 6, false, UUID_TO_BIN('32d80ba0-43f2-4306-a47d-6d70270e1e22'));

-- Insert users
INSERT INTO users (id, first_name, last_name, email, password, enabled, role) VALUES (UUID_TO_BIN('49ecbc0f-d36d-4868-b4f0-ae67de5cd199'), 'Alice', 'Admin', 'alice.admin@footarena.com', '{bcrypt}$2a$12$6yJ261gnjfvq9NRINpnNTOJmLqlW03VYfYILViK83mD4VIlB7Gj9a', true, 'ADMIN');
INSERT INTO users (id, first_name, last_name, email, password, enabled, role) VALUES (UUID_TO_BIN('e7759566-08d7-47fe-9886-3bb582f37ca6'), 'Bob', 'Manager', 'bob.manager@footarena.com', '{bcrypt}$2a$12$6yJ261gnjfvq9NRINpnNTOJmLqlW03VYfYILViK83mD4VIlB7Gj9a', true, 'MANAGER');
INSERT INTO users (id, first_name, last_name, email, password, enabled, role) VALUES (UUID_TO_BIN('1e0fe7a2-0b94-4d72-bd4d-820bc54c3bb6'), 'Charlie', 'Player', 'charlie.player@footarena.com', '{bcrypt}$2a$12$6yJ261gnjfvq9NRINpnNTOJmLqlW03VYfYILViK83mD4VIlB7Gj9a', true, 'PLAYER');
