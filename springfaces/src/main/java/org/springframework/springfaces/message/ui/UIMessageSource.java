/*
 * Copyright 2010-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.springfaces.message.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.springfaces.SpringFacesIntegration;
import org.springframework.springfaces.message.ObjectMessageSource;
import org.springframework.springfaces.message.ObjectMessageSourceUtils;
import org.springframework.springfaces.util.FacesUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Exposes a messages from a Spring {@link MessageSource} for use with JSF pages. The specified {@link #getSource()
 * source} is exposed as a {@link MessageSourceMap} under the specified {@link #getVar() var}. If a source is not
 * explicitly defined the current {@link SpringFacesIntegration#getApplicationContext() application context} will be
 * used.
 * <p>
 * By default the key of message to load is prefixed with a string build from {@link UIViewRoot#getViewId() root view
 * ID} as follows:
 * <ul>
 * <li>Any 'WEB-INF' prefix is removed</li>
 * <li>Any file extension is removed</li>
 * <li>All '/' characters are converted to '.'</li>
 * </ul>
 * For example the prefix the view "<b><tt>/WEB-INF/pages/spring/example.xhtml</tt></b>" will use the prefix "<b>
 * <tt>pages.spring.example.</tt></b> ". Use the {@link #setPrefix(String) prefix} attribute if a different prefix is
 * required.
 * <p>
 * By default this component will attempt to deduce when a return value should be a <tt>String</tt> and when it should
 * be a nested map. This behavior can be changed using the {@link #setReturnStringsWhenPossible(boolean)
 * returnStringsWhenPossible} attribute.
 * 
 * @author Phillip Webb
 * @see MessageSourceMap
 */
public class UIMessageSource extends UIComponentBase {

	private final Log logger = LogFactory.getLog(getClass());

	public static final String COMPONENT_FAMILY = "spring.faces.MessageSource";

	private static final String SLASH = "/";

	private static final String WEB_INF = "WEB-INF";

	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	@Override
	public void setParent(UIComponent parent) {
		super.setParent(parent);
		putMessageSourceInRequestMap(FacesContext.getCurrentInstance());
	}

	private void putMessageSourceInRequestMap(FacesContext context) {
		MessageSourceMap messageSourceMap = createMessageSourceMap(context);
		String var = getVar();
		Assert.state(StringUtils.hasLength(var), "No 'var' attibute specified for UIMessageSource component");
		Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
		Object previous = requestMap.put(var, messageSourceMap);
		if (previous != null && this.logger.isWarnEnabled()) {
			this.logger.warn("The request scoped JSF variable '" + var + "' of type " + previous.getClass().getName()
					+ " has been replaced by UIMessageSource");
		}
	}

	/**
	 * Create a new {@link MessageSourceMap}.
	 * @param context the faces context
	 * @return a {@link MessageSourceMap} instance
	 */
	private MessageSourceMap createMessageSourceMap(final FacesContext context) {
		String[] prefixCodes = getPrefixCodes(context);
		MessageSource messageSource = getSource();
		ApplicationContext applicationContext = getApplicationContext(context);
		Assert.state(((applicationContext != null) || (messageSource != null)),
				"Unable to find MessageSource, ensure that SpringFaces intergation "
						+ "is enabled or set the 'source' attribute");
		ObjectMessageSource objectMessageSource = ObjectMessageSourceUtils.getObjectMessageSource(messageSource,
				applicationContext);
		return new UIMessageSourceMap(context, objectMessageSource, prefixCodes, isReturnStringsWhenPossible());
	}

	private ApplicationContext getApplicationContext(FacesContext context) {
		Assert.notNull(context, "Context must not be null");
		ExternalContext externalContext = context.getExternalContext();
		if (SpringFacesIntegration.isInstalled(externalContext)) {
			return SpringFacesIntegration.getCurrentInstance(externalContext).getApplicationContext();
		}
		return null;
	}

	/**
	 * Returns the prefix codes either has {@link #getPrefix() defined} by the user or built from the root view ID.
	 * @param context the faces context
	 * @return the prefix codes
	 */
	private String[] getPrefixCodes(FacesContext context) {
		String definedPrefix = getPrefix();
		if (definedPrefix != null) {
			return getDefinedPrefixCodes(definedPrefix);
		}
		return new String[] { buildPrefixCodeFromViewRoot(context), "" };
	}

	private String[] getDefinedPrefixCodes(String definedPrefix) {
		List<String> codes = new ArrayList<String>();
		for (String code : StringUtils.commaDelimitedListToStringArray(definedPrefix)) {
			if (StringUtils.hasLength(code)) {
				codes.add(ensureEndsWithDot(code.trim()));
			}
		}
		codes.add("");
		return codes.toArray(new String[codes.size()]);
	}

	/**
	 * Build a prefix code from the current view root.
	 * @param context the faces context
	 * @return a prefix code
	 */
	private String buildPrefixCodeFromViewRoot(FacesContext context) {
		Assert.state(context.getViewRoot() != null, "Unable to build message prefix from null viewRoot");
		Assert.state(context.getViewRoot().getViewId() != null, "Unable to build message prefix from null viewRoot ID");
		String code = context.getViewRoot().getViewId();
		code = removePrefix(code, SLASH);
		code = removePrefix(code, WEB_INF);
		code = removePrefix(code, SLASH);
		code = removeExtension(code);
		code = code.replaceAll("\\/", ".");
		code = ensureEndsWithDot(code);
		return code.toLowerCase();
	}

	private String ensureEndsWithDot(String code) {
		if (!code.endsWith(".")) {
			code = code + ".";
		}
		return code;
	}

	private String removePrefix(String s, String prefix) {
		if (s.toUpperCase().startsWith(prefix)) {
			return s.substring(prefix.length());
		}
		return s;
	}

	private String removeExtension(String s) {
		int lastDot = s.lastIndexOf(".");
		if (lastDot > -1) {
			return s.substring(0, lastDot);
		}
		return s;
	}

	// FIXME DC getters setters and add to taglib.xml

	public String getVar() {
		return (String) getStateHelper().eval(PropertyKeys.var);
	}

	public void setVar(String var) {
		getStateHelper().put(PropertyKeys.var, var);
	}

	public MessageSource getSource() {
		return (MessageSource) getStateHelper().eval(PropertyKeys.source);
	}

	public void setSource(MessageSource source) {
		getStateHelper().put(PropertyKeys.source, source);
	}

	public String getPrefix() {
		return (String) getStateHelper().eval(PropertyKeys.prefix, null);
	}

	public void setPrefix(String prefix) {
		getStateHelper().put(PropertyKeys.prefix, prefix);
	}

	public boolean isReturnStringsWhenPossible() {
		return (Boolean) getStateHelper().eval(PropertyKeys.returnStringsWhenPossible, true);
	}

	public void setReturnStringsWhenPossible(boolean returnStringsWhenPossible) {
		getStateHelper().put(PropertyKeys.returnStringsWhenPossible, returnStringsWhenPossible);
	}

	private enum PropertyKeys {
		source, var, prefix, returnStringsWhenPossible
	}

	private class UIMessageSourceMap extends MessageSourceMap {

		private FacesContext context;

		private boolean returnStringsWhenPossible;

		public UIMessageSourceMap(FacesContext context, MessageSource messageSource, String[] prefixCodes,
				boolean returnStringsWhenPossible) {
			super(messageSource, prefixCodes);
			this.context = context;
			this.returnStringsWhenPossible = returnStringsWhenPossible;
		}

		@Override
		protected Locale getLocale() {
			return FacesUtils.getLocale(this.context);
		}

		@Override
		protected void handleNoSuchMessageException(NoSuchMessageException exception) {
			if (this.context.isProjectStage(ProjectStage.Production)) {
				throw exception;
			}
			if (UIMessageSource.this.logger.isWarnEnabled()) {
				UIMessageSource.this.logger.warn(exception.getMessage(), exception);
			}
			FacesMessage message = new FacesMessage(exception.getMessage());
			message.setSeverity(FacesMessage.SEVERITY_WARN);
			this.context.addMessage(UIMessageSource.this.getClientId(this.context), message);
		}

		@Override
		protected boolean returnStringsWhenPossible() {
			return this.returnStringsWhenPossible;
		}
	}
}
