defmodule Skymibackend.Models.Images do
  use Ecto.Schema
  import Ecto.Changeset
  
  @primary_key {:imageid, Ecto.ULID, autogenerate: true}
  @foreign_key_type Ecto.ULID 

  schema "images" do
    belongs_to :post, Skymibackend.Models.Post,
     foreign_key: :post_id,
     references: :postid
    field :image_path, :string
    timestamps()
  end

  def changeset(images, params \\ %{}) do
    images
     |> cast(params, [:post_id, :image_path])
     |> validate_required([:post_id, :image_path])
     |> foreign_key_constraint(:post_id)
  end
end
