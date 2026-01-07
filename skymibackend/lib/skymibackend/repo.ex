defmodule Skymibackend.Repo do
  use Ecto.Repo,
    otp_app: :skymibackend,
    adapter: Ecto.Adapters.Postgres
end
