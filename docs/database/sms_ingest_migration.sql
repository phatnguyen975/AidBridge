ALTER TABLE public.sos_requests
    ADD COLUMN IF NOT EXISTS client_request_id character varying(100),
    ADD COLUMN IF NOT EXISTS source character varying(30),
    ADD COLUMN IF NOT EXISTS quick_sos boolean,
    ADD COLUMN IF NOT EXISTS accuracy double precision,
    ADD COLUMN IF NOT EXISTS triggered_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS location_captured_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS device_info character varying(500),
    ADD COLUMN IF NOT EXISTS sender_phone character varying(50),
    ADD COLUMN IF NOT EXISTS raw_message text,
    ADD COLUMN IF NOT EXISTS received_at_gateway_millis bigint;

CREATE UNIQUE INDEX IF NOT EXISTS idx_sos_requests_client_request_id
    ON public.sos_requests(client_request_id)
    WHERE client_request_id IS NOT NULL;
