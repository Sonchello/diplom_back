-- Создание таблиц
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rating INTEGER DEFAULT 0,
    avatar_url VARCHAR(255),
    birth_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE requests (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('ACTIVE', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    active_helper_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    help_start_date TIMESTAMP,
    completion_date TIMESTAMP,
    category VARCHAR(255) NOT NULL,
    is_archived BOOLEAN DEFAULT false,
    deadline TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days') NOT NULL,
    is_expired BOOLEAN DEFAULT false,
    deadline_date DATE,
    deadline_type VARCHAR(255)
);

CREATE TABLE help_history (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
    helper_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(255) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_id BIGINT REFERENCES requests(id) ON DELETE CASCADE,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (type IN ('HELP_COMPLETION', 'HELP_CONFIRMATION', 'HELP_CANCELLED', 'HELP_STARTED')),
    status VARCHAR(255) DEFAULT 'UNREAD' NOT NULL CHECK (status IN ('UNREAD', 'READ')),
    action_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_read BOOLEAN DEFAULT false NOT NULL,
    action_needed BOOLEAN DEFAULT false NOT NULL,
    from_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE requests_helpers (
    request_id BIGINT NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
    helpers_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (request_id, helpers_id)
);

CREATE TABLE user_helped_requests (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_id BIGINT NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, request_id)
);

-- Создание индексов
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_requests_user_id ON requests(user_id);
CREATE INDEX idx_requests_active_helper_id ON requests(active_helper_id);
CREATE INDEX idx_requests_status ON requests(status);
CREATE INDEX idx_requests_category ON requests(category);
CREATE INDEX idx_help_history_request_id ON help_history(request_id);
CREATE INDEX idx_help_history_helper_id ON help_history(helper_id);
CREATE INDEX idx_help_history_status ON help_history(status);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_request_id ON notifications(request_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);