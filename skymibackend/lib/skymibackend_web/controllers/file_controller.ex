defmodule SkymibackendWeb.FileController do
  use SkymibackendWeb, :controller

  def userprofileloader(conn, %{"name" => name}) do
    imagepath = Path.join([:code.priv_dir(:skymibackend),"static","uploads","profile_pictures",name])
    if File.exists?(imagepath) do
      conn
        |> put_resp_content_type("image/jpg")
        |> put_resp_header("content-disposition", "inline")
        |> send_file(200, imagepath)
    else
      conn |> send_resp(conn, "400 bad request!")
    end
  end
end
