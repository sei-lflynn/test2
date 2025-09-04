const Sequelize = require("sequelize");
const logger = require("./logger");
const fs = require("fs");
require("dotenv").config();

// create a sequelize instance with our local postgres database information.
const sequelize = new Sequelize(
  process.env.DB_NAME,
  process.env.DB_USER,
  process.env.DB_PASS,
  {
    host: process.env.DB_HOST,
    port: process.env.DB_PORT || "5432",
    dialect: "postgres",
    dialectOptions: {
      ssl:
        process.env.DB_SSL === "true"
          ? {
              require: true,
              rejectUnauthorized: true,
              ca:
                process.env.DB_SSL_CERT_BASE64 != null &&
                process.env.DB_SSL_CERT_BASE64 !== ""
                  ? Buffer.from(
                      process.env.DB_SSL_CERT_BASE64,
                      "base64"
                    ).toString("utf-8")
                  : process.env.DB_SSL_CERT != null &&
                    process.env.DB_SSL_CERT !== ""
                  ? fs.readFileSync(process.env.DB_SSL_CERT)
                  : false,
            }
          : false,
    },
    logging: process.env.VERBOSE_LOGGING == "true" || false,
    pool: {
      max:
        process.env.DB_POOL_MAX != null
          ? parseInt(process.env.DB_POOL_MAX) || 10
          : 10,
      min: 0,
      acquire:
        process.env.DB_POOL_TIMEOUT != null
          ? parseInt(process.env.DB_POOL_TIMEOUT) || 30000
          : 30000,
      idle:
        process.env.DB_POOL_IDLE != null
          ? parseInt(process.env.DB_POOL_IDLE) || 10000
          : 10000,
    },
  }
);

// create a sequelize instance with our local postgres database information.
const sequelizeSTAC =
  process.env.WITH_STAC === "true"
    ? new Sequelize("mmgis-stac", process.env.DB_USER, process.env.DB_PASS, {
        host: process.env.DB_HOST,
        port: process.env.DB_PORT || "5432",
        dialect: "postgres",
        dialectOptions: {
          ssl:
            process.env.DB_SSL === "true"
              ? {
                  require: true,
                  rejectUnauthorized: true,
                  ca:
                    process.env.DB_SSL_CERT_BASE64 != null &&
                    process.env.DB_SSL_CERT_BASE64 !== ""
                      ? Buffer.from(
                          process.env.DB_SSL_CERT_BASE64,
                          "base64"
                        ).toString("utf-8")
                      : process.env.DB_SSL_CERT != null &&
                        process.env.DB_SSL_CERT !== ""
                      ? fs.readFileSync(process.env.DB_SSL_CERT)
                      : false,
                }
              : false,
        },
        logging: process.env.VERBOSE_LOGGING == "true" || false,
        pool: {
          max:
            process.env.DB_POOL_MAX != null
              ? parseInt(process.env.DB_POOL_MAX) || 10
              : 10,
          min: 0,
          acquire:
            process.env.DB_POOL_TIMEOUT != null
              ? parseInt(process.env.DB_POOL_TIMEOUT) || 30000
              : 30000,
          idle:
            process.env.DB_POOL_IDLE != null
              ? parseInt(process.env.DB_POOL_IDLE) || 10000
              : 10000,
        },
      })
    : null;

// Source: http://docs.sequelizejs.com/manual/installation/getting-started.html
sequelize
  .authenticate()
  .then(() => {
    logger(
      "info",
      "Database connection has successfully been established.",
      "connection"
    );
  })
  .catch((err) => {
    logger(
      "infrastructure_error",
      "Unable to connect to the database.",
      "connection",
      null,
      err
    );
  });

module.exports = { sequelize, sequelizeSTAC };
