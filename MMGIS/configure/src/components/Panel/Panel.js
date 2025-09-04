import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import {} from "./PanelSlice";
import { makeStyles } from "@mui/styles";
import mmgisLogo from "../../images/mmgis.png";

import clsx from "clsx";

import { setMission, setModal, setPage, setSnackBarText } from "../../core/ConfigureStore";
import { calls } from "../../core/calls";

import NewMissionModal from "./Modals/NewMissionModal/NewMissionModal";

import Button from "@mui/material/Button";

import TextSnippetIcon from "@mui/icons-material/TextSnippet";
import ShapeLineIcon from "@mui/icons-material/ShapeLine";
import KeyIcon from "@mui/icons-material/Key";
import SettingsIcon from "@mui/icons-material/Settings";
import PhishingIcon from "@mui/icons-material/Phishing";
import ApiIcon from "@mui/icons-material/Api";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import HorizontalSplitIcon from "@mui/icons-material/HorizontalSplit";
import AccountBoxIcon from "@mui/icons-material/AccountBox";

const useStyles = makeStyles((theme) => ({
  Panel: {
    width: "220px",
    height: "100%",
    background: theme.palette.secondary.main,
    backgroundImage: "url(configure/build/contours.png)",
    backgroundSize: "200%",
    display: "flex",
    flexFlow: "column",
  },
  title: {
    padding: "30px 0px",
    textAlign: "center",
  },
  titleImage: {
    height: "30px",
  },
  configurationName: {
    color: theme.palette.swatches.grey[700],
    fontSize: "13px",
    marginTop: "-5px",
    textTransform: "uppercase",
    "& > span": {
      color: theme.palette.accent.main,
    },
  },
  newMission: {
    width: "100%",
  },
  newMissionButton: {
    width: "100%",
    color: `${theme.palette.swatches.grey[900]} !important`,
    background: `${theme.palette.swatches.grey[300]} !important`,
    "&:hover": {
      background: `${theme.palette.swatches.p[0]} !important`,
      color: `${theme.palette.swatches.grey[0]} !important`,
    },
  },
  missions: {
    flex: 1,
    overflowY: "auto",
  },
  missionsUl: {
    listStyleType: "none",
    padding: 0,
    margin: "10px 0px",
    borderTop: `1px solid ${theme.palette.swatches.grey[300]} !important`,
  },
  missionsLi: {
    borderBottom: `1px solid ${theme.palette.swatches.grey[300]} !important`,
  },
  missionButton: {
    width: "100%",
    background: "rgba(255,255,255,0.06) !important",
    color: `${theme.palette.swatches.grey[900]} !important`,
    textTransform: "capitalize !important",
    justifyContent: "end !important",
    fontSize: "16px !important",
    padding: "3px 16px !important",
    transition:
      "background-color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms, width 250ms ease-out 0ms, box-shadow 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms, border-color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms, color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms",
  },
  missionActive: {
    background: `${theme.palette.swatches.grey[1000]} !important`,
    color: `${theme.palette.swatches.grey[100]} !important`,
    fontWeight: "bold !important",
  },
  missionNotActive: {
    "&:hover": {
      background: `${theme.palette.swatches.grey[200]} !important`,
    },
  },
  missionDisabled: {
    opacity: "0.3 !important",
    cursor: "not-allowed !important",
    pointerEvents: "all !important",
    "&:hover": {
      background: "unset !important",
    },
  },
  pages: {
    bottom: "0px",
    display: "flex",
    flexFlow: "column",
    width: "100%",
    borderTop: `2px solid ${theme.palette.swatches.grey[300]}`,
  },
  pageButton: {
    width: "100%",
    borderTop: `1px solid ${theme.palette.swatches.grey[300]} !important`,
    color: `${theme.palette.swatches.grey[700]} !important`,
    textTransform: "capitalize !important",
    justifyContent: "start !important",
    fontSize: "14px !important",
    padding: "3px 16px !important",
    background: "rgba(255,255,255,0.06) !important",
    transition:
      "background-color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms, width 250ms ease-out 0ms, box-shadow 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms, border-color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms, color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms",
    "&:hover": {
      color: `${theme.palette.swatches.grey[900]} !important`,
      background: `${theme.palette.swatches.grey[200]} !important`,
    },
    "& svg": {
      fontSize: "16px !important",
    },
  },
}));

export default function Panel() {
  const c = useStyles();
  const dispatch = useDispatch();

  const missions = useSelector((state) => state.core.missions);
  const activeMission = useSelector((state) => state.core.mission);
  const [userPermissions, setUserPermissions] = useState(null);

  // Fetch user permissions to determine which missions can be edited
  useEffect(() => {
    calls.api(
      "user_permissions",
      {},
      (res) => {
        setUserPermissions(res);
      },
      (res) => {
        dispatch(
          setSnackBarText({
            text: res?.message || "Failed to get user permissions.",
            severity: "error",
          })
        );
      }
    );
  }, [dispatch]);

  const canEditMission = (mission) => {
    if (!userPermissions) return true; // Default to allowing until we know
    if (userPermissions.permission === "111") return true; // SuperAdmin can edit all
    if (userPermissions.permission !== "110") return false; // Non-admins, who can't even access this page, can't edit any
    
    const managingMissions = userPermissions.missions_managing || [];
    return managingMissions.includes(mission);
  };

  return (
    <>
      <div className={c.Panel}>
        <div className={c.title}>
          <img className={c.titleImage} src={mmgisLogo} alt="MMGIS"></img>
          <div className={c.configurationName}>
            <span>Config</span>uration
          </div>
        </div>
        {window.mmgisglobal?.permission === "111" && (
          <div className={c.newMission}>
            <Button
              className={c.newMissionButton}
              variant="contained"
              disableElevation
              onClick={() => {
                dispatch(setModal({ name: "newMission" }));
              }}
            >
              New Mission
            </Button>
          </div>
        )}
        <div className={c.missions}>
          <ul className={c.missionsUl}>
            {missions.map((mission, idx) => {
              const canEdit = canEditMission(mission);
              return (
                <li className={c.missionsLi} key={idx}>
                  <Button
                    className={clsx(
                      {
                        [c.missionActive]: mission === activeMission,
                        [c.missionNotActive]: mission !== activeMission,
                        [c.missionDisabled]: !canEdit,
                      },
                      c.missionButton
                    )}
                    disableElevation
                    onClick={() => {
                      if (canEdit) {
                        dispatch(setMission(mission));
                      } else {
                        dispatch(
                          setSnackBarText({
                            text: `You don't have permission to edit the "${mission}" mission.`,
                            severity: "warning",
                          })
                        );
                      }
                    }}
                    title={canEdit ? mission : `No permission to edit "${mission}"`}
                  >
                    {mission}
                  </Button>
                </li>
              );
            })}
          </ul>
        </div>
        <div className={c.pages}>
          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<ShapeLineIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "geodatasets" }));
            }}
          >
            GeoDatasets
          </Button>
          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<TextSnippetIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "datasets" }));
            }}
          >
            Datasets
          </Button>

          {window.mmgisglobal.WITH_STAC === "true" ? (
            <Button
              className={c.pageButton}
              variant="contained"
              disableElevation
              startIcon={<HorizontalSplitIcon size="small" />}
              onClick={() => {
                dispatch(setMission(null));
                dispatch(setPage({ page: "stac" }));
              }}
            >
              STAC
            </Button>
          ) : null}

          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<KeyIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "api_tokens" }));
            }}
          >
            API Tokens
          </Button>

          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<ApiIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "apis" }));
            }}
          >
            APIs
          </Button>

          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<PhishingIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "webhooks" }));
            }}
          >
            Webhooks
          </Button>

          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<SettingsIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "general_options" }));
            }}
          >
            General Options
          </Button>

          <Button
            className={c.pageButton}
            variant="contained"
            disableElevation
            startIcon={<AccountBoxIcon size="small" />}
            onClick={() => {
              dispatch(setMission(null));
              dispatch(setPage({ page: "users" }));
            }}
          >
            Users
          </Button>
        </div>
      </div>
      <NewMissionModal />
    </>
  );
}
