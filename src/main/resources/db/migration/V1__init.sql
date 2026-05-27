CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    profile_image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    total_reviews INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE handymen (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    bio TEXT,
    profile_image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    token_balance INT DEFAULT 0,
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    total_reviews INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT,
    base_token_cost INT DEFAULT 1
);

INSERT INTO categories (name, slug, base_token_cost) VALUES
('Elektrika', 'elektrika', 2),
('Vodoinstalacije', 'vodoinstalacije', 2),
('Kućno održavanje', 'kucno-odrzavanje', 1),
('Molerski radovi', 'molerski-radovi', 1),
('Stolarija', 'stolarija', 2),
('Keramika i pločice', 'keramika', 2),
('Grejanje i klimatizacija', 'grejanje-klima', 3),
('Bravarski radovi', 'bravarski-radovi', 1),
('Građevinski radovi', 'gradjevinski-radovi', 3),
('Čišćenje i održavanje', 'ciscenje', 1);

CREATE TABLE job_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INT NOT NULL REFERENCES categories(id),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    address TEXT,
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    images TEXT[],
    ai_score INT CHECK (ai_score BETWEEN 1 AND 5),
    token_cost INT NOT NULL DEFAULT 1,
    status VARCHAR(30) DEFAULT 'OPEN',
    selected_handyman_id UUID REFERENCES handymen(id),
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id UUID NOT NULL REFERENCES job_listings(id) ON DELETE CASCADE,
    handyman_id UUID NOT NULL REFERENCES handymen(id),
    tokens_spent INT NOT NULL,
    cover_message TEXT,
    status VARCHAR(30) DEFAULT 'PENDING',
    applied_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(job_listing_id, handyman_id)
);

CREATE TABLE token_packages (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    token_amount INT NOT NULL,
    price_eur DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO token_packages (name, token_amount, price_eur) VALUES
('Starter', 100, 9.99),
('Standard', 200, 17.99),
('Pro', 300, 24.99);

CREATE TABLE token_purchase_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    handyman_id UUID NOT NULL REFERENCES handymen(id),
    package_id INT REFERENCES token_packages(id),
    token_amount INT NOT NULL,
    amount_expected DECIMAL(10, 2) NOT NULL,
    payment_reference VARCHAR(255),
    status VARCHAR(30) DEFAULT 'PENDING',
    admin_note TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE token_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    handyman_id UUID NOT NULL REFERENCES handymen(id),
    job_application_id UUID REFERENCES job_applications(id),
    amount INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id UUID NOT NULL REFERENCES job_listings(id),
    reviewer_type VARCHAR(20) NOT NULL,
    reviewer_user_id UUID,
    reviewer_handyman_id UUID,
    reviewee_user_id UUID,
    reviewee_handyman_id UUID,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(job_listing_id, reviewer_type)
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
