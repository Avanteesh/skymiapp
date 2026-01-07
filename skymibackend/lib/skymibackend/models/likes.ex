defmodule Skymibackend.Models.Likes do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:likeid, Ecto.ULID, autogenerate: true}
  @foreign_key_type Ecto.ULID 

  schema "likes" do
    belongs_to :post, Skymibackend.Models.Post,  # post reference
     foreign_key: :post_id,
     references: :postid
    belongs_to :liked_user, Skymibackend.Models.Users,  # reference to user who liked!
     foreign_key: :liked_user_id,
     references: :userid
    timestamps()
  end

  def changeset(likes, params \\ %{}) do
    likes
      |> cast(params, [:post_id, :liked_user_id])
      |> validate_required([:post_id, :liked_user_id])
      |> foreign_key_constraint(:post_id)
      |> foreign_key_constraint(:liked_user_id)      
  end
end
