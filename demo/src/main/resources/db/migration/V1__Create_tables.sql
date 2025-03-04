-- Создаем таблицу users
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    rating INTEGER DEFAULT 0,
    avatar_url VARCHAR(255),
    birth_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создаем таблицу requests
CREATE TABLE requests (
    id SERIAL PRIMARY KEY,
    description TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    active_helper_id BIGINT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    help_start_date TIMESTAMP,
    completion_date TIMESTAMP,
    category VARCHAR(50) NOT NULL,
    urgency VARCHAR(50) NOT NULL DEFAULT 'medium',
    is_archived BOOLEAN DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (active_helper_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Создаем таблицу user_helped_requests
CREATE TABLE user_helped_requests (
    user_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, request_id)
);

-- Создаем таблицу help_history
CREATE TABLE help_history (
    id SERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    helper_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
    FOREIGN KEY (helper_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Создаем индексы
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_requests_user_id ON requests(user_id);
CREATE INDEX idx_requests_active_helper_id ON requests(active_helper_id);
CREATE INDEX idx_requests_status ON requests(status);
CREATE INDEX idx_requests_category ON requests(category);
CREATE INDEX idx_requests_urgency ON requests(urgency);
CREATE INDEX idx_help_history_helper_id ON help_history(helper_id);
CREATE INDEX idx_help_history_status ON help_history(status);
CREATE INDEX idx_help_history_request_id ON help_history(request_id);

-- Добавляем ограничения
ALTER TABLE requests ADD CONSTRAINT check_status
    CHECK (status IN ('ACTIVE', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

ALTER TABLE requests ADD CONSTRAINT check_urgency
    CHECK (urgency IN ('low', 'medium', 'high'));

ALTER TABLE help_history ADD CONSTRAINT check_help_status
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- Добавляем ограничение уникальности для активных записей
ALTER TABLE help_history ADD CONSTRAINT help_history_unique_active
    UNIQUE (request_id, helper_id, status)
    WHERE status = 'IN_PROGRESS';
    -- Добавляем ограничения на значени

-- Создаем таблицу notifications
CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    request_id BIGINT,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNREAD',
    action_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT false,
    action_needed BOOLEAN NOT NULL DEFAULT false,
    from_user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Создаем индексы для notifications
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_request_id ON notifications(request_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Добавляем ограничение на тип уведомления
ALTER TABLE notifications ADD CONSTRAINT check_notification_type
    CHECK (type IN ('HELP_COMPLETION', 'HELP_CONFIRMATION', 'HELP_CANCELLED', 'HELP_STARTED'));

-- Добавляем ограничение на статус уведомления
ALTER TABLE notifications ADD CONSTRAINT check_notification_status
    CHECK (status IN ('UNREAD', 'READ'));

