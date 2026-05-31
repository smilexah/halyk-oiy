CREATE TABLE population_prior (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_tag   varchar(64) NOT NULL,
    category_name varchar(128) NOT NULL,
    ratio         numeric(5,4) NOT NULL,
    source        varchar(128) NOT NULL,
    UNIQUE (segment_tag, category_name)
);

INSERT INTO population_prior (segment_tag, category_name, ratio, source) VALUES
    ('default', 'Продукты',    0.2500, 'mock_population_2026Q2'),
    ('default', 'Коммуналка',  0.1200, 'mock_population_2026Q2'),
    ('default', 'Транспорт',   0.0800, 'mock_population_2026Q2'),
    ('default', 'Такси',       0.0500, 'mock_population_2026Q2'),
    ('default', 'Рестораны',   0.0900, 'mock_population_2026Q2'),
    ('default', 'Развлечения', 0.0700, 'mock_population_2026Q2'),
    ('default', 'Подписки',    0.0300, 'mock_population_2026Q2'),
    ('default', 'Прочее',      0.2000, 'mock_population_2026Q2'),
    ('high-income', 'Продукты',    0.1500, 'mock_population_2026Q2'),
    ('high-income', 'Рестораны',   0.1500, 'mock_population_2026Q2'),
    ('high-income', 'Путешествия', 0.1500, 'mock_population_2026Q2'),
    ('high-income', 'Прочее',      0.5500, 'mock_population_2026Q2');
