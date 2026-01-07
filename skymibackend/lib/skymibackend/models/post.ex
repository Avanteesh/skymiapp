defmodule Skymibackend.Models.Post do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:postid, Ecto.ULID, autogenerate: true}
  @foreign_key_type Ecto.ULID 

  schema "post" do
    belongs_to :user, Skymibackend.Models.Users,
     foreign_key: :user_id,
     references: :userid
    field :post_title, :string
    field :post_description, :string
    field :post_date, :utc_datetime
    field :bortle_scale, :integer
    field :moon_phase, :string
    timestamps()
  end

  defp validate_moonphase(changeset) do
    value = get_field(changeset, :moon_phase)
    if (value == "New Moon" or value == "Waxing Crescent" or value == "First Quarter" or value == "Waxing Gibbous"
    or value == "Full Moon" or value == "Waning Gibbous" or value == "Last Quarter"or value == "Waning Crescent") do changeset
    else add_error(changeset, :moon_phase, "Not a valid moon phase")
    end
  end

  def changeset(post, params \\ %{}) do
    post
     |> cast(params, [:user_id,:post_title,:post_description,:post_date,:bortle_scale,:moon_phase])
     |> validate_required([:post_title, :post_date, :bortle_scale])
     |> validate_inclusion(:bortle_scale, 1..9) # bortle scale of the location!
     |> validate_moonphase() # check the moon phase when the photograph was captured!
     |> foreign_key_constraint(:user_id)
  end
end
