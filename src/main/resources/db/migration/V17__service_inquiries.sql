CREATE TABLE service_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_slug VARCHAR(120) NOT NULL,
    category_name VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    start_timeline VARCHAR(80) NOT NULL,
    short_description VARCHAR(120),
    detailed_description TEXT NOT NULL,
    salutation VARCHAR(10),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_inquiries_status ON service_inquiries(status);
CREATE INDEX idx_service_inquiries_created_at ON service_inquiries(created_at DESC);
