/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ErrorMessage, Field, Form, Formik } from "formik";
import * as React from "react";
import Button from "components/Button";
import { hasAnyProps } from "lib/lang";

const ChangePasswordFields = ({
                                email,
                                redirectUri,
                                showOldPasswordField,
                                onSubmit,
                                onValidate,
                              }: {
  email?: string;
  redirectUri?: string;
  showOldPasswordField: boolean;
  onSubmit: Function;
  onValidate: (
    oldPassword: string,
    newPassword: string,
    verifyPassword: string,
    email: string,
  ) => Promise<string>;
}) => {
  return (
    <Formik
      enableReinitialize={true}
      initialValues={{
        oldPassword: "",
        newPassword: "",
        verifyPassword: "",
        email: email || "",
        redirectUri: redirectUri || "",
      }}
      onSubmit={values => {
        onSubmit(values);
      }}
      validate={({ oldPassword, newPassword, email, verifyPassword }) =>
        onValidate(oldPassword, newPassword, verifyPassword, email)
      }
    >
      {({ errors, touched }) => {
        const isPristine = !hasAnyProps(touched);
        const hasErrors = hasAnyProps(errors);
        return (
          <Form className="ChangePassword-form">
            <div style={{ display: "none" }}>
              <Field
                className="redirectUri-field"
                name="redirectUri"
                type="hidden"
              />
              <Field className="email-field" name="email" type="hidden"/>
            </div>

            <div className="section__fields">
              <div className="section__fields__row">
                {showOldPasswordField ? (
                  <div className="field-container vertical">
                    <label>Old password</label>
                    <Field name="oldPassword" type="password" autoFocus/>
                    <ErrorMessage
                      name="oldPassword"
                      render={msg => (
                        <div className="validation-error">{msg}</div>
                      )}
                    />
                  </div>
                ) : (
                  <div className="field-container vertical"/>
                )}

                <div className="field-container__spacer"/>

                <div className="field-container vertical">
                  <label>New password</label>
                  <Field name="newPassword" type="password"/>
                  <ErrorMessage
                    name="newPassword"
                    render={msg => (
                      <div className="validation-error">{msg}</div>
                    )}
                  />
                </div>
              </div>

              <div className="section__fields__row">
                <div className="field-container vertical"/>
                <div className="field-container__spacer"/>
                <div className="field-container vertical">
                  <label>New password again</label>
                  <Field name="verifyPassword" type="password"/>
                  <ErrorMessage
                    name="verifyPassword"
                    render={msg => (
                      <div className="validation-error">{msg}</div>
                    )}
                  />
                </div>
              </div>

              <div className="ChangePassword-controls">
                <div className="ChangePassword-actions">
                  <Button
                    disabled={isPristine || hasErrors}
                    action="primary"
                    appearance="contained"
                    type="submit"
                    icon="save"
                    text="Change password"
                  />
                </div>
              </div>
            </div>
          </Form>
        );
      }}
    </Formik>
  );
};

export default ChangePasswordFields;
