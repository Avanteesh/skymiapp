defmodule Skymibackend.JWT do
  use Joken.Config
  import Nvir
  
  def token_config do
    default_claims(
      iss: "skymibackend",
      aud: "skymibackend_users",
      default_exp: 60*60*5  # 5 hours till expiration!
    )
  end

  def signer do
    Joken.Signer.create("HS256", env!("SECRET_KEY"))
  end
end
